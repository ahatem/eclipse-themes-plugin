# Disposable R9 runner (derived from the R8 v8 harness): unmodified Eclipse install, the Tycho-built product jar of HEAD,
# the r9 probe, a fresh configuration and workspace per scenario, sandboxed user.home.
param([string]$Tag,[string]$StepsFile,[string]$Eclipse,[string]$Cfg,[string]$Ws)
$ErrorActionPreference='Stop'
$v=(Resolve-Path $PSScriptRoot).Path
$install=(Resolve-Path "$v\..\..\eclipse-$Eclipse\eclipse").Path
$cfg="$v\cfg-$Cfg"; $ws="$v\ws-$Ws"; $out="$v\out"
New-Item -ItemType Directory -Force $cfg,$out,"$v\home","$ws\.metadata\.plugins\org.eclipse.core.runtime\.settings" | Out-Null
$prefFile="$ws\.metadata\.plugins\org.eclipse.core.runtime\.settings\org.eclipse.ui.workbench.prefs"
if(-not(Test-Path $prefFile)){Set-Content $prefFile "eclipse.preferences.version=1`nPLUGINS_NOT_ACTIVATED_ON_STARTUP=org.eclipse.ui.win32;" -Encoding ascii}
if(-not(Test-Path "$cfg\bundles.info")){
 $lines=Get-Content "$install\configuration\org.eclipse.equinox.simpleconfigurator\bundles.info" | ForEach-Object {if($_ -like 'org.eclipse.equinox.p2.reconciler.dropins,*'){$_ -replace ',4,true$',',-1,false'}else{$_}}
 $lines+="com.github.eclipsethemes,1.1.0.202610191106,$([Uri]::new("$v/com.github.eclipsethemes_1.1.0.jar").AbsoluteUri),4,false"
 $lines+="spike.r9probe,9.0.0,$([Uri]::new("$v/spike.r9probe_9.0.0.jar").AbsoluteUri),4,false"
 Set-Content "$cfg\bundles.info" $lines -Encoding ascii
 $ini=Get-Content "$install\configuration\config.ini" | Where-Object {$_ -notlike 'org.eclipse.equinox.simpleconfigurator.configUrl=*' -and $_ -notlike 'eclipse.p2.data.area=*'}
 $ini+="org.eclipse.equinox.simpleconfigurator.configUrl=$([Uri]::new("$cfg/bundles.info").AbsoluteUri)"
 $ini+='eclipse.p2.data.area=@config.dir/p2/'
 Set-Content "$cfg\config.ini" $ini -Encoding ascii
}
$vm=@("-Dorg.eclipse.equinox.simpleconfigurator.configUrl=$([Uri]::new("$cfg/bundles.info").AbsoluteUri)","-Dosgi.configuration.cascaded=false","-Duser.home=$v\home","-Dprobe.tag=$Tag","-Dprobe.out=$out","-Dprobe.dir=$v","-Dprobe.stepsfile=$StepsFile")
$args=@('-clean','-nosplash','-consoleLog','-data',$ws,'-configuration',$cfg,'-vmargs')+$vm
Remove-Item "$out\$Tag.log","$out\$Tag-uia.log" -ErrorAction SilentlyContinue
$p=Start-Process "$install\eclipse.exe" -ArgumentList $args -PassThru -RedirectStandardOutput "$out\$Tag.stdout" -RedirectStandardError "$out\$Tag.stderr"
"$Tag pid=$($p.Id)"
if(-not $p.WaitForExit(600000)){ "$Tag TIMEOUT-KILL"; $p.Kill($true) }
"$Tag exit=$($p.ExitCode)"
if(Test-Path "$out\$Tag.log"){Get-Content "$out\$Tag.log"}
if(Test-Path "$out\$Tag-uia.log"){Get-Content "$out\$Tag-uia.log"}
