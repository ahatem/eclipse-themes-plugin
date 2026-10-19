$ErrorActionPreference='Stop'
$v=(Resolve-Path $PSScriptRoot).Path
$jdk='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin'
$cp=(Resolve-Path "$v\..\..\eclipse-4.40\eclipse\plugins").Path+'\*'
Remove-Item "$v\classes" -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory "$v\classes" | Out-Null
& "$jdk\javac.exe" --release 17 -nowarn -cp $cp -d "$v\classes" "$v\src\spike\p8\Probe.java"
if($LASTEXITCODE){throw "javac failed"}
& "$jdk\jar.exe" --create --file "$v\spike.p8probe_8.0.0.jar" --manifest "$v\META-INF\MANIFEST.MF" -C "$v\classes" . -C $v plugin.xml
if($LASTEXITCODE){throw "jar failed"}
