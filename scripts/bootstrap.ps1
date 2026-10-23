param([switch]$SkipMySql, [switch]$SkipJava)
$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot
$downloadDir = Join-Path $projectDir '.downloads'
$runtimeDir = Join-Path $projectDir '.runtime'
New-Item -ItemType Directory -Path $downloadDir,$runtimeDir -Force | Out-Null
$curlNetworkArgs = @('-4')
$proxyConfig = Get-ItemProperty -LiteralPath 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings' -ErrorAction SilentlyContinue
if ($proxyConfig.ProxyEnable -eq 1 -and $proxyConfig.ProxyServer -match '^127\.0\.0\.1:[0-9]+$') {
    $curlNetworkArgs += @('--proxy', ('http://' + $proxyConfig.ProxyServer))
}
function Get-Archive([string]$url, [string]$file, [string]$algorithm, [string]$expected) {
    if (-not (Test-Path -LiteralPath $file)) {
        Write-Host ("Downloading " + [IO.Path]::GetFileName($file))
        & curl.exe @curlNetworkArgs --silent --show-error --fail --location --retry 2 --connect-timeout 20 --output ($file + '.part') $url
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $url" }
        Move-Item -LiteralPath ($file + '.part') -Destination $file -Force
    }
    $actual = (Get-FileHash -LiteralPath $file -Algorithm $algorithm).Hash.ToLowerInvariant()
    if ($actual -ne $expected.ToLowerInvariant()) { throw "Checksum mismatch. Remove the archive and retry: $file" }
}
$jdkDir = Join-Path $runtimeDir 'jdk-17.0.9'
if (-not $SkipJava -and -not (Test-Path -LiteralPath (Join-Path $jdkDir 'bin\java.exe'))) {
    $jdkUrl = 'https://cdn.azul.com/zulu/bin/zulu17.46.19-ca-jdk17.0.9-win_x64.zip'
    $jdkHash = '32e110628ea0bd750b84e0f937e7c98874505e76a0590d759565dfa803f89ccf'
    $jdkZip = Join-Path $downloadDir 'jdk-17.0.9.zip'
    Get-Archive $jdkUrl $jdkZip 'SHA256' $jdkHash
    Expand-Archive -LiteralPath $jdkZip -DestinationPath $runtimeDir -Force
    $extractedJdk = Join-Path $runtimeDir 'zulu17.46.19-ca-jdk17.0.9-win_x64'
    foreach ($jdkPath in @($extractedJdk,$jdkDir)) {
        if (-not ([IO.Path]::GetFullPath($jdkPath).StartsWith([IO.Path]::GetFullPath($runtimeDir) + '\',[StringComparison]::OrdinalIgnoreCase))) { throw 'Invalid runtime path' }
    }
    Move-Item -LiteralPath $extractedJdk -Destination $jdkDir
}
if (-not $SkipMySql -and -not (Test-Path -LiteralPath (Join-Path $runtimeDir 'mysql-8.4.11-winx64\bin\mysqld.exe'))) {
    $mysqlZip = Join-Path $downloadDir 'mysql-8.4.11-winx64.zip'
    Get-Archive 'https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.11-winx64.zip' $mysqlZip 'MD5' '2e833921898a9a030ea6bfe81bd811bc'
    Expand-Archive -LiteralPath $mysqlZip -DestinationPath $runtimeDir -Force
}
. (Join-Path $PSScriptRoot 'env.ps1')
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -version
Write-Host 'Portable runtime ready. Next: scripts/start-mysql.ps1, then scripts/init-db.ps1.'
