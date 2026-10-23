param([string]$Avd='Pixel_6',[int]$Port=5558)
. (Join-Path $PSScriptRoot 'env.ps1')
if($Port -lt 5554 -or $Port -gt 5682 -or $Port % 2 -ne 0){throw 'Use an even emulator port between 5554 and 5682.'}
$sdkRoot=@($env:ANDROID_HOME,$env:ANDROID_SDK_ROOT,'D:\Android\Sdk',(Join-Path $env:LOCALAPPDATA 'Android\Sdk')) | Where-Object {$_ -and (Test-Path -LiteralPath (Join-Path $_ 'emulator\emulator.exe'))} | Select-Object -First 1
if(-not $sdkRoot){throw 'Set ANDROID_HOME and create an Android virtual device in Android Studio first.'}
$adb=Join-Path $sdkRoot 'platform-tools\adb.exe'
$emulator=Join-Path $sdkRoot 'emulator\emulator.exe'
$serial='emulator-'+$Port
$devices=& $adb devices
if(-not($devices -match ($serial+'\s+device'))){
    $avdList=@(& $emulator -list-avds)
    if($avdList -notcontains $Avd){throw ('Android virtual device not found: '+$Avd+'. Available: '+($avdList -join ', '))}
    # This is an interactive emulator intended for the user to see and control.
    Start-Process -FilePath $emulator -ArgumentList @('-avd',$Avd,'-read-only','-no-snapshot','-no-audio','-gpu','swiftshader_indirect','-port',$Port) -WindowStyle Normal | Out-Null
}
$ready=$false
for($attempt=0;$attempt -lt 120;$attempt++){
    $deviceState=$null
    try{$deviceState=& $adb -s $serial get-state 2>$null}catch{}
    if($deviceState -eq 'device'){
        $boot=& $adb -s $serial shell getprop sys.boot_completed 2>$null
        if(($boot -join '').Trim() -eq '1'){$ready=$true;break}
    }
    Start-Sleep -Seconds 2
}
if(-not $ready){throw 'Android emulator did not finish booting within 4 minutes.'}
& (Join-Path $PSScriptRoot 'run-android.ps1') -Device $serial
Write-Host ('App running on '+$serial+'. Default backend: http://10.0.2.2:8080')
