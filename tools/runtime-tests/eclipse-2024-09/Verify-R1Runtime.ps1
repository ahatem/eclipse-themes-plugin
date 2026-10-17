[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string] $RuntimeDirectory,
    [Parameter(Mandatory = $true)][string] $JdkHome,
    [Parameter(Mandatory = $true)][string] $ProductJar,
    [Parameter(Mandatory = $true)][string] $SourceRevision,
    [ValidateSet('All', 'Core', 'Consumers', 'Absent')][string] $Check = 'All',
    [ValidateSet('None', 'Assertion', 'Listener', 'MissingCompletion', 'Timeout')][string] $FailureControl = 'None',
    [ValidateRange(1, 3600)][int] $TimeoutSeconds = 120
)
. (Join-Path $PSScriptRoot 'RuntimeSupport.ps1')
$FailureControl = @{ None = 'None'; Assertion = 'Assertion'; Listener = 'Listener'; MissingCompletion = 'MissingCompletion'; Timeout = 'Timeout' }[$FailureControl]
if ($FailureControl -ne 'None' -and $Check -notin @('All', 'Consumers')) {
    throw 'Consumer failure controls require -Check Consumers or All; they cannot be counted as passing core/absent checks.'
}
$runtime = Get-R1Path $RuntimeDirectory
$product = Get-R1Path $ProductJar
$tools = Get-R1Tools $JdkHome
if (-not (Test-Path -LiteralPath $product -PathType Leaf)) { throw "Built product jar not found: $product" }
$state = Get-Content -LiteralPath (Join-Path $runtime 'runtime.json') -Raw | ConvertFrom-Json
if ($state.schema -ne 1 -or $state.owner -ne 'eclipse-themes-r1' -or $state.root -ne $runtime -or $state.status -ne 'prepared') {
    throw 'Expected explicitly prepared, identified R1 runtime state, not a prior arbitrary Eclipse installation.'
}
$baseline = Read-R1Baseline
foreach ($key in $baseline.Keys) {
    if ($state.baseline.$key -ne $baseline[$key]) { throw "Prepared baseline differs from tracked inputs: $key" }
}
foreach ($kind in @('present', 'absent')) {
    $profile = Get-R1Path (Join-Path $runtime $kind)
    Assert-R1Profile $profile $kind $baseline
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($product)
try {
    $reader = [IO.StreamReader]::new($zip.GetEntry('META-INF/MANIFEST.MF').Open())
    try { $manifest = $reader.ReadToEnd().Replace("`r", '').Replace("`n ", '') } finally { $reader.Dispose() }
    # Tycho/bnd expresses the source BREE as an equivalent osgi.ee capability.
    $java17 = $manifest -match '(?m)^Bundle-RequiredExecutionEnvironment: JavaSE-17$' -or
        $manifest -match '(?m)^Require-Capability: .*osgi.ee.*\(osgi.ee=JavaSE\).*\(version=17\)'
    if ($manifest -notmatch '(?m)^Bundle-SymbolicName: com.github.eclipsethemes(;|$)' -or -not $java17) {
        throw 'Incorrect product artifact identity/JavaSE-17 requirement.'
    }
} finally { $zip.Dispose() }

$run = Join-Path $runtime ('run-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $run | Out-Null
$report = @{ sourceRevision = $SourceRevision; productJar = $product;
    productSha256 = (Get-FileHash -LiteralPath $product -Algorithm SHA256).Hash;
    jdk = (Get-R1Path $JdkHome); baseline = $baseline; check = $Check; failureControl = $FailureControl;
    status = 'running'; checks = @(); output = $run }
$reportPath = Join-Path $run 'result.json'
Write-R1Json $reportPath $report
try {
    Copy-Item -LiteralPath $product -Destination (Join-Path $run 'product.jar')
    $repo = Get-R1Path (Join-Path $PSScriptRoot '../../..')
    foreach ($kind in @('present', 'absent')) {
        $build = New-Item -ItemType Directory -Path (Join-Path $run "build-$kind")
        $classes = New-Item -ItemType Directory -Path (Join-Path $build.FullName 'classes')
        $sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot "$kind/src") -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
        $sources += @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'common/src') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
        if ($kind -eq 'present') {
            foreach ($helper in @('XmlProviderSpy', 'LoopbackCanary')) {
                $sources += Join-Path $repo "bundles/com.github.eclipsethemes/test/com/github/eclipsethemes/core/parser/$helper.java"
            }
        }
        $arguments = @('--release', '17', '-encoding', 'UTF-8', '-cp', (Join-Path $runtime 'present/plugins/*'), '-d', $classes.FullName) + $sources
        Invoke-R1Process $tools.javac $arguments $build.FullName (Join-Path $run "compile-$kind") $TimeoutSeconds
        Invoke-R1Process $tools.jar @('cfm', (Join-Path $run "$kind.jar"), (Join-Path $PSScriptRoot "$kind/META-INF/MANIFEST.MF"),
            '-C', $classes.FullName, '.', '-C', (Join-Path $PSScriptRoot $kind), 'plugin.xml') $build.FullName (Join-Path $run "jar-$kind") $TimeoutSeconds
    }
    $jobs = @(
        @{ name = 'core'; kind = 'present'; app = 'r1.present.core'; marker = 'R1 CORE COMPLETE'; workspace = 'core' },
        @{ name = 'persistence'; kind = 'present'; app = 'r1.present.core'; marker = 'R1 PERSISTENCE COMPLETE'; workspace = 'core' },
        @{ name = 'consumers'; kind = 'present'; app = 'r1.present.consumers'; marker = 'R1 CONSUMERS COMPLETE'; workspace = 'consumers' },
        @{ name = 'absent'; kind = 'absent'; app = 'r1.absent.run'; marker = 'R1 ABSENT COMPLETE'; workspace = 'absent' }
    )
    foreach ($job in $jobs) {
        if ($Check -eq 'Core' -and $job.name -notin @('core', 'persistence')) { continue }
        if ($Check -eq 'Consumers' -and $job.name -ne 'consumers') { continue }
        if ($Check -eq 'Absent' -and $job.name -ne 'absent') { continue }
        $profile = Join-Path $runtime $job.kind
        $config = Join-Path $run ("config-" + $job.workspace)
        if (-not (Test-Path -LiteralPath $config)) {
            New-Item -ItemType Directory -Path (Join-Path $config 'org.eclipse.equinox.simpleconfigurator') -Force | Out-Null
            Copy-Item -LiteralPath (Join-Path $profile 'configuration/config.ini') -Destination (Join-Path $config 'config.ini')
            $info = Get-Content -LiteralPath (Join-Path $profile 'configuration/org.eclipse.equinox.simpleconfigurator/bundles.info') -Raw
            $productLocation = ([Uri](Join-Path $run 'product.jar')).AbsoluteUri
            $probeLocation = ([Uri](Join-Path $run "$($job.kind).jar")).AbsoluteUri
            $version = [regex]::Match($manifest, '(?m)^Bundle-Version: ([^\r\n]+)').Groups[1].Value
            $info += "`ncom.github.eclipsethemes,$version,$productLocation,4,false`nr1.$($job.kind),1.0.0,$probeLocation,4,false`n"
            [IO.File]::WriteAllText((Join-Path $config 'org.eclipse.equinox.simpleconfigurator/bundles.info'), $info, [Text.UTF8Encoding]::new($false))
        }
        $log = Join-Path $run $job.name
        $entry = @{ name = $job.name; status = 'running'; log = $log }
        $report.checks += $entry
        Write-R1Json $reportPath $report
        $arguments = @("-Duser.home=$run/user-$($job.workspace)", "-Dr1.scratch=$run", "-Dr1.probeFault=$FailureControl",
            "-Dr1.resetPhase=$(if ($job.name -eq 'persistence') { 'verify' } else { 'exercise' })",
            '-jar', (Get-R1Launcher $profile), '-install', $profile, '-configuration', $config,
            '-data', (Join-Path $run "workspace-$($job.workspace)"), '-user', (Join-Path $run "user-$($job.workspace)"),
            '-clean', '-nosplash', '-consoleLog', '-application', $job.app)
        Invoke-R1Process $tools.java $arguments $run $log $TimeoutSeconds
        if (-not (Select-String -LiteralPath "$log.stdout.log" -Pattern "^$([regex]::Escape($job.marker))$" -Quiet)) {
            throw "Missing completed assertions marker for $($job.name). Logs: $log"
        }
        $entry.status = 'passed'
        Write-R1Json $reportPath $report
    }
    $report.status = 'passed'
    Write-R1Json $reportPath $report
    Write-Output "R1 runtime verification passed ($Check). Results and unfiltered logs: $run"
} catch {
    $report.status = 'failed'; $report.error = $_.ToString()
    foreach ($entry in $report.checks) { if ($entry.status -eq 'running') { $entry.status = 'failed' } }
    Write-R1Json $reportPath $report
    throw
}
