[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string] $BootstrapEclipse,
    [Parameter(Mandatory = $true)][string] $JdkHome,
    [Parameter(Mandatory = $true)][string] $OutputDirectory
)
. (Join-Path $PSScriptRoot 'RuntimeSupport.ps1')
$output = Get-R1Path $OutputDirectory
$bootstrap = Get-R1Path $BootstrapEclipse
$jdk = Get-R1Path $JdkHome
Assert-R1FreshOutput $output $bootstrap $jdk
$tools = Get-R1Tools $jdk
New-Item -ItemType Directory -Path $output | Out-Null
$shell = Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
$prepare = Join-Path $PSScriptRoot 'Prepare-R1Runtime.ps1'
$repo = Get-R1Path (Join-Path $PSScriptRoot '../../..')

function Expect-Rejected([string] $Name, [string] $Target, [string] $JdkInput = $jdk) {
    $log = Join-Path $output $Name
    $rejected = $false
    try {
        Invoke-R1Process $shell @('-NoProfile', '-File', $prepare, '-BootstrapEclipse', $bootstrap,
            '-JdkHome', $JdkInput, '-OutputDirectory', $Target) $output $log 30
    } catch { $rejected = $true }
    if (-not $rejected) { throw "Unsafe/missing input unexpectedly accepted: $Name" }
    $expected = @{ source = 'source tree'; bootstrap = 'overlaps caller-supplied input'; jdk = 'overlaps caller-supplied input';
        existing = 'new directory'; workspace = 'Eclipse workspace'; reparse = 'Reparse path rejected'; 'missing-jdk' = 'Missing caller-supplied JDK tool' }
    if (-not (Select-String -LiteralPath "$log.stderr.log" -Pattern $expected[$Name] -Quiet)) {
        throw "Wrong rejection reason for $Name. Logs: $log"
    }
    Write-Output "PASS rejection: $Name"
}

$unsafeSource = Join-Path $repo ('r1-must-not-create-' + [Guid]::NewGuid().ToString('N'))
Expect-Rejected 'source' $unsafeSource
if (Test-Path -LiteralPath $unsafeSource) { throw 'Source output was mutated' }
foreach ($inputName in @('bootstrap', 'jdk')) {
    $base = if ($inputName -eq 'bootstrap') { $bootstrap } else { $jdk }
    $unsafe = Join-Path $base ('r1-must-not-create-' + [Guid]::NewGuid().ToString('N'))
    Expect-Rejected $inputName $unsafe
    if (Test-Path -LiteralPath $unsafe) { throw "Caller input was mutated: $unsafe" }
}
$existing = New-Item -ItemType Directory -Path (Join-Path $output 'unowned')
[IO.File]::WriteAllText((Join-Path $existing.FullName 'sentinel'), 'preserve')
Expect-Rejected 'existing' $existing.FullName
if ([IO.File]::ReadAllText((Join-Path $existing.FullName 'sentinel')) -ne 'preserve') { throw 'Unowned state changed' }
$workspace = New-Item -ItemType Directory -Path (Join-Path $output 'workspace/.metadata') -Force
$workspaceChild = Join-Path $workspace.Parent.FullName 'child'
Expect-Rejected 'workspace' $workspaceChild
if (Test-Path -LiteralPath $workspaceChild) { throw 'Workspace output mutated' }
$junction = Join-Path $output 'junction'
New-Item -ItemType Junction -Path $junction -Target $existing.FullName | Out-Null
Expect-Rejected 'reparse' (Join-Path $junction 'child')
if (Test-Path -LiteralPath (Join-Path $existing.FullName 'child')) { throw 'Reparse target mutated' }
$missingOutput = Join-Path $output 'missing-tool-output'
Expect-Rejected 'missing-jdk' $missingOutput (Join-Path $output 'missing-jdk')
if (Test-Path -LiteralPath $missingOutput) { throw 'Missing tool created output before validation' }

$badProfile = Join-Path $output 'incompatible-profile'
$badMetadata = Join-Path $badProfile 'configuration/org.eclipse.equinox.simpleconfigurator'
New-Item -ItemType Directory -Path $badMetadata -Force | Out-Null
$inputs = Read-R1Baseline
$badLines = foreach ($id in @('org.eclipse.platform', 'org.eclipse.osgi', 'org.eclipse.jdt.ui', 'org.eclipse.pde.ui', 'org.eclipse.ant.ui')) {
    $version = if ($id -eq 'org.eclipse.pde.ui') { '0.0.0' } else { $inputs[$id] }
    "$id,$version,plugins/unused.jar,4,false"
}
[IO.File]::WriteAllLines((Join-Path $badMetadata 'bundles.info'), $badLines)
$incompatible = $false
try { Assert-R1Profile $badProfile 'present' $inputs }
catch { $incompatible = $_.ToString().Contains('Baseline mismatch/missing bundle org.eclipse.pde.ui') }
if (-not $incompatible) { throw 'Incompatible PDE baseline was not rejected' }
Write-Output 'PASS incompatible PDE baseline is rejected before runtime launch'

$timedOut = $false
try { Invoke-R1Process $shell @('-NoProfile', '-Command', 'Start-Sleep -Seconds 30') $output (Join-Path $output 'timeout') 1 }
catch { $timedOut = $_.ToString().Contains('timed out') }
if (-not $timedOut -or -not (Test-Path -LiteralPath (Join-Path $output 'timeout.stderr.log'))) {
    throw 'Process timeout did not fail with retained logs'
}
Write-Output 'PASS timeout terminates the owned process and retains logs'

# Exercise the real preparation error path with deliberately unreachable repository input.
# Only an owned copy of the tracked preparation sources is changed; real sources stay intact.
$failureSource = Join-Path $output 'failure-source/tools/runtime-tests/eclipse-2024-09'
New-Item -ItemType Directory -Path $failureSource -Force | Out-Null
foreach ($name in @('Prepare-R1Runtime.ps1', 'RuntimeSupport.ps1', 'baseline.properties')) {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot $name) -Destination (Join-Path $failureSource $name)
}
$failureBaseline = Join-Path $failureSource 'baseline.properties'
$text = [IO.File]::ReadAllText($failureBaseline)
[IO.File]::WriteAllText($failureBaseline, ($text -replace '(?m)^repository=.*$', 'repository=http://127.0.0.1:1/unreachable'))
$failedOutput = Join-Path $output 'failed-download'
$failed = $false
try {
    Invoke-R1Process $shell @('-NoProfile', '-File', (Join-Path $failureSource 'Prepare-R1Runtime.ps1'),
        '-BootstrapEclipse', $bootstrap, '-JdkHome', $jdk, '-OutputDirectory', $failedOutput,
        '-TimeoutSeconds', '60') $output (Join-Path $output 'failed-download-control') 90
} catch { $failed = $true }
$failure = Get-Content -LiteralPath (Join-Path $failedOutput 'runtime.json') -Raw | ConvertFrom-Json
if (-not $failed -or $failure.status -ne 'failed' -or
    -not (Test-Path -LiteralPath (Join-Path $failedOutput 'prepare-present.stderr.log'))) {
    throw 'Failed preparation/download did not leave diagnostic non-success state'
}
Write-Output 'PASS failed repository preparation is non-success with retained diagnostics'
Write-Output "R1 SCRIPT CONTROLS COMPLETE: $output"
