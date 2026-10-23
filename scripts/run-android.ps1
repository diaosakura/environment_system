param([string]$Device)
. (Join-Path $PSScriptRoot 'env.ps1')
$sdkRoot=@($env:ANDROID_HOME,$env:ANDROID_SDK_ROOT,'D:\Android\Sdk',(Join-Path $env:LOCALAPPDATA 'Android\Sdk')) | Where-Object {$_ -and (Test-Path -LiteralPath (Join-Path $_ 'platform-tools\adb.exe'))} | Select-Object -First 1
if(-not $sdkRoot){throw 'Set ANDROID_HOME to your Android SDK path.'}
$adb=Join-Path $sdkRoot 'platform-tools\adb.exe'
$deviceArgs=@();if($Device){$deviceArgs=@('-s',$Device)}
$apk=Join-Path $ProjectRoot 'release\qingjing-nepg-1.1.0.apk'
if(-not(Test-Path -LiteralPath $apk)){throw 'Build the App first with scripts/build-mobile.ps1.'}
& $adb @deviceArgs install -r $apk
if($LASTEXITCODE -ne 0){throw 'APK installation failed. Start an Android emulator, or connect one Android device with USB debugging.'}
& $adb @deviceArgs shell am start -n edu.net.environment.grid/.MainActivity
if($LASTEXITCODE -ne 0){throw 'App launch failed'}
