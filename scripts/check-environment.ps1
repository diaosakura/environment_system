param([switch]$Development,[switch]$Android)
$ErrorActionPreference='Stop'
$projectDir=Split-Path -Parent $PSScriptRoot
$missingRequired=0
function Report([string]$name,[string]$status,[string]$detail){Write-Host ('[{0}] {1}: {2}' -f $status,$name,$detail)}
function Native-Version([string]$exe,[string]$arguments){
    $psi=New-Object Diagnostics.ProcessStartInfo
    $psi.FileName=$exe;$psi.Arguments=$arguments;$psi.UseShellExecute=$false
    $psi.RedirectStandardOutput=$true;$psi.RedirectStandardError=$true;$psi.CreateNoWindow=$true
    $p=New-Object Diagnostics.Process;$p.StartInfo=$psi
    try{$p.Start()|Out-Null;$stdout=$p.StandardOutput.ReadToEnd();$stderr=$p.StandardError.ReadToEnd();$p.WaitForExit();return (($stdout+$stderr).Trim() -split '\r?\n')[0]}finally{$p.Dispose()}
}
$javaRoots=@((Join-Path $projectDir '.runtime\jdk-17.0.9'),$env:JAVA_HOME)
$javaRoot=$javaRoots | Where-Object {$_ -and(Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe'))} | Select-Object -First 1
if($javaRoot){Report 'Java' 'OK' (Native-Version (Join-Path $javaRoot 'bin\java.exe') '-version')}else{Report 'Java' 'MISSING' 'Run scripts/bootstrap.ps1 or set JAVA_HOME.';$missingRequired++}
$mysqlRoots=@($env:MYSQL_HOME,(Join-Path $projectDir '.runtime\mysql-8.4.11-winx64'))
$mysqlRoot=$mysqlRoots | Where-Object {$_ -and(Test-Path -LiteralPath (Join-Path $_ 'bin\mysql.exe'))} | Select-Object -First 1
if($mysqlRoot){Report 'MySQL client' 'OK' (Native-Version (Join-Path $mysqlRoot 'bin\mysql.exe') '--version')}else{Report 'MySQL client' 'MISSING' 'Run bootstrap or set MYSQL_HOME for init/restore scripts.';$missingRequired++}
foreach($file in @('target\environment_system-1.0.0.jar','release\qingjing-nepg-1.1.0.apk','release\qingjing-neps-1.1.0-weixin.zip')){
    if(Test-Path -LiteralPath (Join-Path $projectDir $file)){Report 'Artifact' 'OK' $file}else{Report 'Artifact' 'MISSING' $file;$missingRequired++}
}
$serverPort=if($env:SERVER_PORT){$env:SERVER_PORT}else{'8080'}
try{$health=Invoke-RestMethod -Uri ('http://localhost:'+ $serverPort +'/api/health') -TimeoutSec 3;if($health.data.status -eq 'UP'){Report 'Backend/database health' 'OK' ('UP on '+$serverPort)}else{Report 'Backend/database health' 'INFO' 'Not UP; inspect logs.'}}catch{Report 'Backend/database health' 'INFO' 'Not running or not reachable yet. Start MySQL and the app first.'}
if($Development){
    $nodeCommand=Get-Command node.exe -ErrorAction SilentlyContinue
    if($nodeCommand){Report 'Node' 'OK' (Native-Version $nodeCommand.Source '--version')}else{Report 'Node' 'MISSING' 'Install Node 24.x for source builds.';$missingRequired++}
    foreach($file in @('mvnw.cmd','.mvn\wrapper\maven-wrapper.properties','web\package-lock.json','clients\neps-miniprogram\package-lock.json','clients\nepg-app\package-lock.json')){if(Test-Path -LiteralPath (Join-Path $projectDir $file)){Report 'Build input' 'OK' $file}else{Report 'Build input' 'MISSING' $file;$missingRequired++}}
}
if($Android){
    $sdkRoots=@($env:ANDROID_HOME,$env:ANDROID_SDK_ROOT,(Join-Path $env:LOCALAPPDATA 'Android\Sdk'),'D:\Android\Sdk')
    $sdkRoot=$sdkRoots | Where-Object {$_ -and(Test-Path -LiteralPath (Join-Path $_ 'platform-tools\adb.exe'))} | Select-Object -First 1
    if($sdkRoot){foreach($file in @('platform-tools\adb.exe','platforms\android-34\android.jar','build-tools\34.0.0\aapt.exe','build-tools\34.0.0\d8.bat','build-tools\34.0.0\apksigner.bat')){if(Test-Path -LiteralPath (Join-Path $sdkRoot $file)){Report 'Android build tool' 'OK' $file}else{Report 'Android build tool' 'MISSING' $file;$missingRequired++}}}else{Report 'Android SDK' 'MISSING' 'Set ANDROID_HOME; install Platform 34 and Build Tools 34.0.0.';$missingRequired++}
}
Report 'WeChat' 'MANUAL' 'Install DevTools, sign in, and choose a permitted test AppID. Service port is only needed for CLI/automation.'
if($missingRequired){Write-Host ('Required items missing: '+$missingRequired);exit 1}
Write-Host 'Required files and runtime programs found. Use START_HERE.md for initialization and end-to-end acceptance.'
