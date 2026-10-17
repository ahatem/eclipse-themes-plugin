[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string] $BootstrapEclipse,
    [Parameter(Mandatory = $true)][string] $JdkHome,
    [Parameter(Mandatory = $true)][string] $OutputDirectory,
    [ValidateRange(1, 3600)][int] $TimeoutSeconds = 600
)
. (Join-Path $PSScriptRoot 'RuntimeSupport.ps1')

# Validate every caller path and prerequisite BEFORE creating any output.
$bootstrap = Get-R1Path $BootstrapEclipse
$jdk = Get-R1Path $JdkHome
$output = Get-R1Path $OutputDirectory
Assert-R1FreshOutput $output $bootstrap $jdk
$tools = Get-R1Tools $jdk
$launcher = Get-R1Launcher $bootstrap
$bootstrapConfig = Get-R1Path (Join-Path $bootstrap 'configuration/config.ini')
$bootstrapBundles = Get-R1Path (Join-Path $bootstrap 'configuration/org.eclipse.equinox.simpleconfigurator/bundles.info')
foreach ($inputFile in @($bootstrapConfig, $bootstrapBundles)) {
    if (-not (Test-Path -LiteralPath $inputFile -PathType Leaf)) { throw "Missing bootstrap launch metadata: $inputFile" }
}
if (-not (Get-ChildItem -LiteralPath (Join-Path $bootstrap 'plugins') -Filter 'org.eclipse.equinox.p2.director_*.jar')) {
    throw 'The caller-supplied Eclipse bootstrap must contain p2 director.'
}
$baseline = Read-R1Baseline
$inputHashes = @{}
foreach ($inputFile in @($launcher, $bootstrapConfig, $bootstrapBundles,
        (Join-Path $bootstrap 'p2/pools.info'), (Join-Path $bootstrap 'p2/profiles.info'))) {
    if (Test-Path -LiteralPath $inputFile -PathType Leaf) {
        $inputHashes[$inputFile] = (Get-FileHash -LiteralPath $inputFile -Algorithm SHA256).Hash
    }
}
New-Item -ItemType Directory -Path $output -ErrorAction Stop | Out-Null
$state = @{ schema = 1; owner = 'eclipse-themes-r1'; root = $output; status = 'preparing';
    baseline = $baseline }
Write-R1Json (Join-Path $output 'runtime.json') $state
try {
    # Copy the bootstrap's runnable bundle configuration; all writable state is relocated.
    # Its plugin jars are read-only inputs. Neither its p2 profile nor its workspace is used.
    foreach ($kind in @('present', 'absent')) {
        $scratch = New-Item -ItemType Directory -Path (Join-Path $output "bootstrap-$kind")
        $configuration = New-Item -ItemType Directory -Path (Join-Path $scratch.FullName 'configuration')
        $simple = New-Item -ItemType Directory -Path (Join-Path $configuration.FullName 'org.eclipse.equinox.simpleconfigurator')
        Copy-Item -LiteralPath $bootstrapConfig -Destination (Join-Path $configuration.FullName 'config.ini')
        Copy-Item -LiteralPath $bootstrapBundles -Destination (Join-Path $simple.FullName 'bundles.info')
        $destination = Join-Path $output $kind
        $arguments = @("-Duser.home=$($scratch.FullName)/user", '-jar', $launcher,
            '-install', $bootstrap, '-configuration', (Join-Path $scratch.FullName 'configuration'), '-data', (Join-Path $scratch.FullName 'workspace'),
            '-user', (Join-Path $scratch.FullName 'user'), '-nosplash', '-consoleLog', '-application', 'org.eclipse.equinox.p2.director',
            '-repository', $baseline.repository, '-installIU', $baseline["${kind}IUs"], '-destination', $destination,
            '-bundlepool', $destination, '-profile', "R1-$kind", '-roaming', '-p2.os', 'win32', '-p2.ws', 'win32', '-p2.arch', 'x86_64')
        Invoke-R1Process $tools.java $arguments $scratch.FullName (Join-Path $output "prepare-$kind") $TimeoutSeconds
        Assert-R1Profile $destination $kind $baseline
    }
    foreach ($inputFile in $inputHashes.Keys) {
        if ((Get-FileHash -LiteralPath $inputFile -Algorithm SHA256).Hash -ne $inputHashes[$inputFile]) {
            throw "Bootstrap input changed during preparation; cannot claim isolated stable inputs: $inputFile"
        }
    }
    $state.status = 'prepared'
    Write-R1Json (Join-Path $output 'runtime.json') $state
    Write-Output "Prepared fresh R1 present and platform-only absent profiles: $output"
} catch {
    $state.status = 'failed'
    $state.error = $_.ToString()
    Write-R1Json (Join-Path $output 'runtime.json') $state
    throw
}
