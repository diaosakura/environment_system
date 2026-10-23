param([Parameter(Mandatory=$true)][string]$OutputDirectory)
$ErrorActionPreference='Stop'
$projectDir=Split-Path -Parent $PSScriptRoot
$outputDir=[IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
$stageDir=Join-Path $projectDir 'release\windows-x64-env-20261020'
if(Test-Path -LiteralPath $stageDir){throw 'Environment staging folder exists; choose a fresh export after inspecting it.'}
$zipPath=Join-Path $outputDir 'environment-system-20261020-windows-x64-env.zip'
if(Test-Path -LiteralPath $zipPath){throw 'Environment ZIP already exists.'}
New-Item -ItemType Directory -Path $stageDir,(Join-Path $stageDir 'archives'),(Join-Path $stageDir 'installers') -Force | Out-Null
$downloads=Join-Path $projectDir '.downloads'
$packages=@(
    @{Name='jdk-17.0.9.zip';Folder='archives';Algorithm='SHA256';Hash='32e110628ea0bd750b84e0f937e7c98874505e76a0590d759565dfa803f89ccf'},
    @{Name='mysql-8.4.11-winx64.zip';Folder='archives';Algorithm='MD5';Hash='2e833921898a9a030ea6bfe81bd811bc'},
    @{Name='wechat_devtools_2.02.2609231_win32_x64.exe';Folder='installers';Algorithm='MD5';Hash='72282e4cfd8d602b06caf790189d6383'}
)
foreach($package in $packages){
    $source=Join-Path $downloads $package.Name
    if(-not(Test-Path -LiteralPath $source)){throw ('Required cached official archive missing: '+$package.Name)}
    if((Get-FileHash -LiteralPath $source -Algorithm $package.Algorithm).Hash -ne $package.Hash){throw ('Official archive checksum mismatch: '+$package.Name)}
    Copy-Item -LiteralPath $source -Destination (Join-Path (Join-Path $stageDir $package.Folder) $package.Name)
    Write-Host ('Verified and copied: '+$package.Name)
}
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'offline\prepare-runtime.ps1') -Destination (Join-Path $stageDir 'prepare-runtime.ps1')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'offline\README.md') -Destination (Join-Path $stageDir 'README.md')
$lines=Get-ChildItem -LiteralPath $stageDir -Recurse -File | Sort-Object FullName | ForEach-Object {(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()+'  '+$_.FullName.Substring($stageDir.Length+1).Replace('\','/')}
[IO.File]::WriteAllText((Join-Path $stageDir 'SHA256SUMS.txt'),(($lines -join [char]10)+[char]10),(New-Object Text.UTF8Encoding($false)))
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$archive=[IO.Compression.ZipFile]::Open($zipPath,[IO.Compression.ZipArchiveMode]::Create)
try{
    Get-ChildItem -LiteralPath $stageDir -Recurse -File | ForEach-Object {
        $relative=$_.FullName.Substring($stageDir.Length+1).Replace('\','/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$_.FullName,$relative,[IO.Compression.CompressionLevel]::NoCompression)|Out-Null
    }
}finally{$archive.Dispose()}
Write-Host ('Environment ZIP: '+$zipPath)
