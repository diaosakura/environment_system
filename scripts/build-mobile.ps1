param([switch]$SkipMiniProgram,[switch]$SkipApp,[switch]$SkipInstall,[string]$WeChatAppId=$env:WECHAT_APPID)
. (Join-Path $PSScriptRoot 'env.ps1')
if (-not $SkipMiniProgram) {
    Push-Location (Join-Path $ProjectRoot 'clients\neps-miniprogram')
    try {
        if (-not $SkipInstall -and -not (Test-Path 'node_modules')) { & npm.cmd ci --no-audit --no-fund; if($LASTEXITCODE -ne 0){throw 'Mini-program dependency installation failed'} }
        & npm.cmd run build:mp-weixin
        if($LASTEXITCODE -ne 0){throw 'Mini-program build failed'}
        # Empty AppID is intentional: select your own test AppID in WeChat DevTools.
        $appIdValue='touristappid';if($WeChatAppId){$appIdValue=$WeChatAppId}
        $projectConfig = @{appid=$appIdValue;projectname='qingjing-neps';compileType='miniprogram';miniprogramRoot='./';setting=@{urlCheck=$false;es6=$true;minified=$true};libVersion='latest'}
        $json = $projectConfig | ConvertTo-Json -Depth 5
        [IO.File]::WriteAllText((Join-Path (Get-Location) 'dist\build\mp-weixin\project.config.json'),$json,(New-Object Text.UTF8Encoding($false)))
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $releaseDir=Join-Path $ProjectRoot 'release'
        New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null
        $miniZip=Join-Path $releaseDir 'qingjing-neps-1.1.0-weixin.zip'
        if(Test-Path -LiteralPath $miniZip){Remove-Item -LiteralPath $miniZip -Force}
        [IO.Compression.ZipFile]::CreateFromDirectory((Join-Path (Get-Location) 'dist\build\mp-weixin'),$miniZip)
        Write-Host 'NEPS: import clients/neps-miniprogram/dist/build/mp-weixin into WeChat DevTools; choose a test AppID.'
    } finally { Pop-Location }
}
if (-not $SkipApp) {
    Push-Location (Join-Path $ProjectRoot 'clients\nepg-app')
    try {
        if (-not $SkipInstall -and -not (Test-Path 'node_modules')) { & npm.cmd ci --no-audit --no-fund; if($LASTEXITCODE -ne 0){throw 'App dependency installation failed'} }
        & npm.cmd run build
        if($LASTEXITCODE -ne 0){throw 'App frontend build failed'}
    } finally { Pop-Location }
    & (Join-Path $PSScriptRoot 'build-android.ps1')
}
