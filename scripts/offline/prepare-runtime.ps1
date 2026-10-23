param([Parameter(Mandatory=$true)][string]$ProjectPath)
$ErrorActionPreference='Stop'
$bundleRoot=$PSScriptRoot
$manifestPath=Join-Path $bundleRoot 'SHA256SUMS.txt'
if(-not(Test-Path -LiteralPath $manifestPath)){throw 'Extract the entire environment ZIP before running this script.'}
foreach($line in Get-Content -LiteralPath $manifestPath -Encoding UTF8){
    if(-not $line.Trim()){continue}
    if($line -notmatch '^([a-fA-F0-9]{64})  (.+)$'){throw 'Invalid environment manifest'}
    $expected=$Matches[1];$relative=$Matches[2]
    $candidate=[IO.Path]::GetFullPath((Join-Path $bundleRoot $relative))
    if(-not $candidate.StartsWith([IO.Path]::GetFullPath($bundleRoot)+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Invalid archive path'}
    if(-not(Test-Path -LiteralPath $candidate)){throw ('Environment file missing: '+$relative)}
    if((Get-FileHash -LiteralPath $candidate -Algorithm SHA256).Hash -ne $expected){throw ('Environment checksum mismatch: '+$relative)}
}
$resolvedProject=(Resolve-Path -LiteralPath $ProjectPath).Path
$bootstrap=Join-Path $resolvedProject 'scripts\bootstrap.ps1'
if(-not(Test-Path -LiteralPath $bootstrap)){throw 'ProjectPath must point to the extracted environment_system root.'}
$downloadDir=Join-Path $resolvedProject '.downloads'
New-Item -ItemType Directory -Path $downloadDir -Force | Out-Null
foreach($name in @('jdk-17.0.9.zip','mysql-8.4.11-winx64.zip')){
    $source=Join-Path $bundleRoot ('archives\'+$name);$destination=Join-Path $downloadDir $name
    if(Test-Path -LiteralPath $destination){if((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ne (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash){throw ('Existing project cache differs; inspect it before replacing: '+$destination)}}else{Copy-Item -LiteralPath $source -Destination $destination}
}
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $bootstrap
if($LASTEXITCODE -ne 0){throw 'Runtime preparation failed; inspect bootstrap output.'}
Write-Host 'JDK and MySQL are ready. This script did not initialize or replace database records.'
Write-Host 'Next: follow project START_HERE.md, choose demo initialization OR restore your own SQL backup.'
Write-Host ('WeChat installer (manual): '+(Join-Path $bundleRoot 'installers\wechat_devtools_2.02.2609231_win32_x64.exe'))
