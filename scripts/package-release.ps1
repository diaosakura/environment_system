param([string]$Version='1.1.0',[string]$ArchiveName,[string]$OutputDirectory)
. (Join-Path $PSScriptRoot 'env.ps1')
if($Version -notmatch '^\d+\.\d+\.\d+$'){throw 'Invalid release version'}
if(-not $ArchiveName){$ArchiveName='environment-system-'+$Version+'-source-and-app.zip'}
if($ArchiveName -notmatch '^[a-zA-Z0-9._-]+\.zip$'){throw 'ArchiveName must be a plain ZIP filename.'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$artifactDir=Join-Path $ProjectRoot 'release'
$releaseDir=$artifactDir
if($OutputDirectory){$releaseDir=[IO.Path]::GetFullPath($OutputDirectory)}
New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null
$zipPath=Join-Path $releaseDir $ArchiveName
if(Test-Path -LiteralPath $zipPath){throw 'Archive exists; choose another filename before packaging.'}
$items=New-Object 'System.Collections.Generic.List[System.IO.FileInfo]'
foreach($folder in @('src\main\java','src\main\resources\mapper','src\test','web\src','web\public','web\scripts','clients\neps-miniprogram\src','clients\nepg-app\src','clients\nepg-app\android\src','clients\nepg-app\android\res','sql','scripts','config','docs','.mvn')){
    Get-ChildItem -LiteralPath (Join-Path $ProjectRoot $folder) -File -Recurse | ForEach-Object {$items.Add($_)}
}
$rootFiles=@('README.md','START_HERE.md','START_HERE.html','pom.xml','mvnw','mvnw.cmd','.gitignore','.vscode\extensions.json','.vscode\tasks.json','src\main\resources\application.yml','src\main\resources\static\.gitkeep','web\package.json','web\package-lock.json','web\vite.config.js','web\index.html','web\.prettierrc.json','clients\neps-miniprogram\package.json','clients\neps-miniprogram\package-lock.json','clients\neps-miniprogram\vite.config.js','clients\nepg-app\package.json','clients\nepg-app\package-lock.json','clients\nepg-app\vite.config.js','clients\nepg-app\index.html','clients\nepg-app\android\AndroidManifest.xml','target\environment_system-1.0.0.jar','release\qingjing-nepg-1.1.0.apk','release\qingjing-neps-1.1.0-weixin.zip')
foreach($relative in $rootFiles){$items.Add((Get-Item -LiteralPath (Join-Path $ProjectRoot $relative)))}
$uniqueItems=@($items | Sort-Object FullName -Unique)
$contentLines=foreach($item in $uniqueItems){
    $relative=$item.FullName.Substring($ProjectRoot.Length+1).Replace('\','/')
    (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash.ToLowerInvariant()+'  '+$relative
}
$manifestPath=Join-Path $artifactDir 'CONTENTS-SHA256SUMS.txt'
[IO.File]::WriteAllText($manifestPath,(($contentLines -join [char]10)+[char]10),(New-Object Text.UTF8Encoding($false)))
$archive=[IO.Compression.ZipFile]::Open($zipPath,[IO.Compression.ZipArchiveMode]::Create)
try{
    foreach($item in $uniqueItems){$relative=$item.FullName.Substring($ProjectRoot.Length+1).Replace('\','/');[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$item.FullName,('environment_system/'+$relative),[IO.Compression.CompressionLevel]::Optimal)|Out-Null}
    [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$manifestPath,'environment_system/release/CONTENTS-SHA256SUMS.txt',[IO.Compression.CompressionLevel]::Optimal)|Out-Null
}finally{$archive.Dispose()}
$checksumLines=@()
if($releaseDir -ne $artifactDir){foreach($artifact in @('qingjing-nepg-1.1.0.apk','qingjing-neps-1.1.0-weixin.zip')){Copy-Item -LiteralPath (Join-Path $artifactDir $artifact) -Destination (Join-Path $releaseDir $artifact) -Force}}
foreach($relative in @($ArchiveName,'qingjing-nepg-1.1.0.apk','qingjing-neps-1.1.0-weixin.zip')){$checksumLines+=(Get-FileHash -LiteralPath (Join-Path $releaseDir $relative) -Algorithm SHA256).Hash.ToLowerInvariant()+'  '+$relative}
[IO.File]::WriteAllText((Join-Path $releaseDir 'SHA256SUMS.txt'),(($checksumLines -join [char]10)+[char]10),(New-Object Text.UTF8Encoding($false)))
Write-Host ('Release: '+$zipPath)
Write-Host ('Content files: '+$uniqueItems.Count+' plus SHA256 manifest. Runtime/data/caches and local settings excluded.')
