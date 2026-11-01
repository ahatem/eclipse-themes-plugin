# Builds the probe bundle and runs it in a stock Eclipse install next to the plugin jar.
# Usage: run.ps1 -Eclipse 4.40 -PluginJar <path> -Work <scratch dir> -Out <evidence out dir> -Tag <name>
param([string]$Eclipse,[string]$PluginJar,[string]$Work,[string]$Out,[string]$Tag)
$ErrorActionPreference='Stop'
$v=(Resolve-Path $PSScriptRoot).Path
$repo=(Resolve-Path "$v\..\..\..\..\..\..").Path
$install="$repo\tmp\eclipse-$Eclipse\eclipse"
$jdk='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin'
$w="$Work\$Tag"; Remove-Item $w -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$w\classes","$w\cfg","$w\home",$Out | Out-Null
& "$jdk\javac.exe" --release 17 -nowarn -cp "$install\plugins\*" -d "$w\classes" "$v\src\probe\gap\Probe.java"
if($LASTEXITCODE){throw "javac failed"}
& "$jdk\jar.exe" --create --file "$w\probe.gap_1.0.0.jar" --manifest "$v\META-INF\MANIFEST.MF" -C "$w\classes" . -C $v plugin.xml
if($LASTEXITCODE){throw "jar failed"}
$pj=(Resolve-Path $PluginJar).Path
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z=[IO.Compression.ZipFile]::OpenRead($pj); $mf=(New-Object IO.StreamReader($z.GetEntry('META-INF/MANIFEST.MF').Open())).ReadToEnd(); $z.Dispose()
$ver=([regex]'Bundle-Version: *(\S+)').Match($mf).Groups[1].Value
$lines=Get-Content "$install\configuration\org.eclipse.equinox.simpleconfigurator\bundles.info" | ForEach-Object {if($_ -like 'org.eclipse.equinox.p2.reconciler.dropins,*'){$_ -replace ',4,true$',',-1,false'}else{$_}}
$lines+="com.github.eclipsethemes,$ver,$([Uri]::new($pj).AbsoluteUri),4,false"
$lines+="probe.gap,1.0.0,$([Uri]::new("$w\probe.gap_1.0.0.jar").AbsoluteUri),4,false"
Set-Content "$w\cfg\bundles.info" $lines -Encoding ascii
$ini=Get-Content "$install\configuration\config.ini" | Where-Object {$_ -notlike 'org.eclipse.equinox.simpleconfigurator.configUrl=*' -and $_ -notlike 'eclipse.p2.data.area=*'}
$ini+="org.eclipse.equinox.simpleconfigurator.configUrl=$([Uri]::new("$w\cfg\bundles.info").AbsoluteUri)"
$ini+='eclipse.p2.data.area=@config.dir/p2/'
Set-Content "$w\cfg\config.ini" $ini -Encoding ascii
$vm=@("-Dosgi.configuration.cascaded=false","-Duser.home=$w\home","-Dprobe.tag=$Tag","-Dprobe.out=$Out")
$args=@('-clean','-nosplash','-consoleLog','-data',"$w\ws",'-configuration',"$w\cfg",'-vmargs')+$vm
Remove-Item "$Out\$Tag.log" -ErrorAction SilentlyContinue
$p=Start-Process "$install\eclipse.exe" -ArgumentList $args -PassThru -RedirectStandardOutput "$w\stdout.txt" -RedirectStandardError "$w\stderr.txt"
if(-not $p.WaitForExit(240000)){ "$Tag TIMEOUT-KILL"; $p.Kill($true) }
"$Tag exit=$($p.ExitCode) bundle=$ver"
if(Test-Path "$Out\$Tag.log"){Get-Content "$Out\$Tag.log"} else { Get-Content "$w\stderr.txt" -Tail 40 }
