# R1-only script helpers. Compatible with Windows PowerShell 5.1; no external modules.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-R1Path([string] $Path) {
    if ([string]::IsNullOrWhiteSpace($Path)) { throw 'An explicit path is required.' }
    $full = [IO.Path]::GetFullPath($Path).TrimEnd('\', '/')
    if ($full -notmatch '^[A-Za-z]:\\') { throw "Expected a local absolute Windows path: $full" }
    $part = $full
    while ($part) {
        if (Test-Path -LiteralPath $part) {
            $item = Get-Item -LiteralPath $part -Force
            if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Reparse path rejected: $part" }
        }
        $parent = [IO.Path]::GetDirectoryName($part)
        if ($parent -eq $part) { break }
        $part = $parent
    }
    return $full
}

function Test-R1Within([string] $Path, [string] $Parent) {
    return $Path.Equals($Parent, [StringComparison]::OrdinalIgnoreCase) -or
        $Path.StartsWith($Parent.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)
}

function Assert-R1FreshOutput([string] $Output, [string] $Bootstrap, [string] $Jdk) {
    $root = Get-R1Path (Join-Path $PSScriptRoot '../../..')
    if (Test-Path -LiteralPath $Output) { throw "Output must be a new directory, not existing state: $Output" }
    if ((Test-R1Within $root $Output) -or (Test-R1Within $Output $root)) {
        throw "Output would overwrite or enter the source tree: $Output"
    }
    foreach ($inputPath in @($Bootstrap, $Jdk)) {
        if ((Test-R1Within $Output $inputPath) -or (Test-R1Within $inputPath $Output)) {
            throw "Output overlaps caller-supplied input: $inputPath"
        }
    }
    # Never accept a workspace as generated output, even outside this repository.
    $parent = [IO.Path]::GetDirectoryName($Output)
    while ($parent) {
        if (Test-Path -LiteralPath (Join-Path $parent '.metadata')) { throw "Output is inside an Eclipse workspace: $parent" }
        $parent = [IO.Path]::GetDirectoryName($parent)
    }
}

function Get-R1Tools([string] $JdkHome) {
    $jdk = Get-R1Path $JdkHome
    $tools = @{}
    foreach ($tool in @('java', 'javac', 'jar')) {
        $path = Join-Path $jdk "bin/$tool.exe"
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing caller-supplied JDK tool: $path" }
        $tools[$tool] = $path
    }
    $release = Get-Content -LiteralPath (Join-Path $jdk 'release') -Raw
    if ($release -notmatch 'JAVA_VERSION="21[.\"]') { throw 'The bounded R1 launcher/build requires JDK 21.' }
    return $tools
}

function Read-R1Baseline {
    $values = @{}
    foreach ($line in Get-Content -LiteralPath (Join-Path $PSScriptRoot 'baseline.properties')) {
        if ($line -match '^([^#=]+)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
    }
    return $values
}

function ConvertTo-R1Argument([string] $Value) {
    # Windows CommandLineToArgvW quoting, including embedded quotes and final slashes.
    return '"' + [regex]::Replace([regex]::Replace($Value, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"'
}

function Invoke-R1Process([string] $Executable, [string[]] $Arguments, [string] $Directory,
        [string] $Log, [int] $TimeoutSeconds = 600) {
    if ($TimeoutSeconds -lt 1) { throw 'Timeout must be positive.' }
    $quoted = ($Arguments | ForEach-Object { ConvertTo-R1Argument $_ }) -join ' '
    [IO.File]::WriteAllText("$Log.command.txt", "$Executable $quoted`r`n")
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $Executable
    $start.Arguments = $quoted
    $start.WorkingDirectory = $Directory
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $start
    $null = $process.Start()
    # Drain both streams concurrently; neither a full stderr pipe nor stdout can deadlock.
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    try {
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            # Only the process tree created by this call is terminated. No file cleanup.
            & "$env:SystemRoot/System32/taskkill.exe" /PID $process.Id /T /F | Out-Null
            $null = $process.WaitForExit(5000)
            throw "Runtime process timed out after $TimeoutSeconds seconds. Logs: $Log"
        }
        if ($process.ExitCode -ne 0) { throw "Process exited $($process.ExitCode). Logs: $Log" }
    } finally {
        if ($process.HasExited) {
            [IO.File]::WriteAllText("$Log.stdout.log", $stdout.GetAwaiter().GetResult())
            [IO.File]::WriteAllText("$Log.stderr.log", $stderr.GetAwaiter().GetResult())
        }
        $process.Dispose()
    }
}

function Get-R1Launcher([string] $Eclipse) {
    $launchers = @(Get-ChildItem -LiteralPath (Join-Path $Eclipse 'plugins') -Filter 'org.eclipse.equinox.launcher_*.jar')
    if ($launchers.Count -ne 1) { throw "Expected one Equinox launcher under $Eclipse" }
    return $launchers[0].FullName
}

function Write-R1Json([string] $Path, $Value) {
    [IO.File]::WriteAllText($Path, ($Value | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
}

function Assert-R1Profile([string] $Eclipse, [string] $Kind, $Baseline) {
    $info = Join-Path $Eclipse 'configuration/org.eclipse.equinox.simpleconfigurator/bundles.info'
    if (-not (Test-Path -LiteralPath $info)) { throw "Missing provisioned profile metadata: $info" }
    $lines = Get-Content -LiteralPath $info
    $ids = @('org.eclipse.platform', 'org.eclipse.osgi')
    if ($Kind -eq 'present') { $ids += @('org.eclipse.jdt.ui', 'org.eclipse.pde.ui', 'org.eclipse.ant.ui') }
    foreach ($id in $ids) {
        if (-not ($lines | Where-Object { $_.StartsWith("$id,$($Baseline[$id]),") })) {
            throw "Baseline mismatch/missing bundle $id $($Baseline[$id]) in $Kind profile"
        }
    }
    # Platform itself includes the batch compiler for Ant. Availability is the editor
    # bundle contract, not an org.eclipse.jdt.* name-prefix heuristic.
    if ($Kind -eq 'absent' -and ($lines | Where-Object { $_ -match '^org.eclipse.(jdt|pde).ui,' })) {
        throw 'Platform-only profile unexpectedly includes JDT/PDE UI integrations.'
    }
}
