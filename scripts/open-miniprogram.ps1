param([string]$AppId)
. (Join-Path $PSScriptRoot 'env.ps1')
$miniDir=Join-Path $ProjectRoot 'clients\neps-miniprogram\dist\build\mp-weixin'
if(-not(Test-Path -LiteralPath (Join-Path $miniDir 'app.json'))){throw 'Compile the mini-program first: scripts/build-mobile.ps1 -SkipApp'}
if($AppId){
    if($AppId -notmatch '^wx[a-zA-Z0-9]{16}$'){throw 'Expected a WeChat AppID starting with wx followed by 16 characters.'}
    $configPath=Join-Path $miniDir 'project.config.json'
    $config=Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $config.appid=$AppId
    [IO.File]::WriteAllText($configPath,($config|ConvertTo-Json -Depth 10),(New-Object Text.UTF8Encoding($false)))
}
$toolRoots=@($env:WECHAT_DEVTOOLS_HOME,(Join-Path $ProjectRoot '.runtime\wechat-devtools'),'C:\Program Files (x86)\Tencent\WeChatDevTools')
$toolRoot=$toolRoots | Where-Object {$_ -and (Test-Path -LiteralPath (Join-Path $_ 'cli.bat'))} | Select-Object -First 1
if(-not $toolRoot){Write-Host ('Import this directory into WeChat DevTools: '+$miniDir);throw 'Install WeChat DevTools, or set WECHAT_DEVTOOLS_HOME.'}
Write-Host 'Sign in and enable the local service port in WeChat DevTools settings before using CLI.'
& (Join-Path $toolRoot 'cli.bat') open --project $miniDir --lang zh
Write-Host ('If CLI is unavailable, import this directory manually: '+$miniDir)
