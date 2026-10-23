. (Join-Path $PSScriptRoot 'env.ps1')
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$sdkCandidates=@($env:ANDROID_HOME,$env:ANDROID_SDK_ROOT,'D:\Android\Sdk',(Join-Path $env:LOCALAPPDATA 'Android\Sdk'))
$sdkRoot=$sdkCandidates | Where-Object {$_ -and (Test-Path -LiteralPath (Join-Path $_ 'platforms\android-34\android.jar'))} | Select-Object -First 1
if(-not $sdkRoot){throw 'Set ANDROID_HOME to an Android SDK containing Android 34 and Build Tools 34.0.0.'}
$toolsDir=Join-Path $sdkRoot 'build-tools\34.0.0'
if(-not(Test-Path -LiteralPath (Join-Path $toolsDir 'aapt.exe'))){throw 'Install Android SDK Build Tools 34.0.0 first.'}
$clientDir=Join-Path $ProjectRoot 'clients\nepg-app'
$androidDir=Join-Path $clientDir 'android'
$buildDir=Join-Path $androidDir 'build'
$resolvedBuild=[IO.Path]::GetFullPath($buildDir)
if(-not $resolvedBuild.StartsWith([IO.Path]::GetFullPath($androidDir)+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Unsafe build path'}
if(Test-Path -LiteralPath $buildDir){Remove-Item -LiteralPath $buildDir -Recurse -Force}
New-Item -ItemType Directory -Path $buildDir,(Join-Path $buildDir 'classes'),(Join-Path $buildDir 'dex') -Force | Out-Null
$androidJar=Join-Path $sdkRoot 'platforms\android-34\android.jar'
$assets=Join-Path $clientDir 'dist'
if(-not(Test-Path -LiteralPath (Join-Path $assets 'index.html'))){throw 'Run the App frontend build first.'}
$unsigned=Join-Path $buildDir 'unsigned.apk'
& (Join-Path $toolsDir 'aapt.exe') package -f -M (Join-Path $androidDir 'AndroidManifest.xml') -S (Join-Path $androidDir 'res') -A $assets -I $androidJar -F $unsigned
if($LASTEXITCODE -ne 0){throw 'Android resources packaging failed'}
$sources=@(Get-ChildItem -LiteralPath (Join-Path $androidDir 'src') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
& (Join-Path $env:JAVA_HOME 'bin\javac.exe') -encoding UTF-8 -source 8 -target 8 -bootclasspath ($androidJar+';'+(Join-Path $toolsDir 'core-lambda-stubs.jar')) -d (Join-Path $buildDir 'classes') $sources
if($LASTEXITCODE -ne 0){throw 'Android Java compilation failed'}
$classesJar=Join-Path $buildDir 'classes.jar'
& (Join-Path $env:JAVA_HOME 'bin\jar.exe') cf $classesJar -C (Join-Path $buildDir 'classes') .
if($LASTEXITCODE -ne 0){throw 'Classes JAR creation failed'}
& (Join-Path $toolsDir 'd8.bat') --min-api 26 --lib $androidJar --output (Join-Path $buildDir 'dex') $classesJar
if($LASTEXITCODE -ne 0){throw 'D8 compilation failed'}
$archive=[IO.Compression.ZipFile]::Open($unsigned,[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,(Join-Path $buildDir 'dex\classes.dex'),'classes.dex',[IO.Compression.CompressionLevel]::Optimal)|Out-Null}finally{$archive.Dispose()}
$aligned=Join-Path $buildDir 'aligned.apk'
& (Join-Path $toolsDir 'zipalign.exe') -f 4 $unsigned $aligned
if($LASTEXITCODE -ne 0){throw 'APK alignment failed'}
$signingDir=Join-Path $ProjectRoot 'data\android-signing'
New-Item -ItemType Directory -Path $signingDir -Force | Out-Null
$keyPath=Join-Path $signingDir 'qingjing-demo.keystore'
if(-not(Test-Path -LiteralPath $keyPath)){
    & (Join-Path $env:JAVA_HOME 'bin\keytool.exe') -genkeypair -keystore $keyPath -storepass android -keypass android -alias qingjing-demo -dname 'CN=Qingjing Course Demo,O=Environment System,C=CN' -keyalg RSA -keysize 2048 -validity 3650
    if($LASTEXITCODE -ne 0){throw 'Demo signing key creation failed'}
}
$releaseDir=Join-Path $ProjectRoot 'release'
New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null
$apk=Join-Path $releaseDir 'qingjing-nepg-1.1.0.apk'
& (Join-Path $toolsDir 'apksigner.bat') sign --ks $keyPath --ks-key-alias qingjing-demo --ks-pass pass:android --key-pass pass:android --out $apk $aligned
if($LASTEXITCODE -ne 0){throw 'APK signing failed'}
& (Join-Path $toolsDir 'apksigner.bat') verify --verbose $apk
if($LASTEXITCODE -ne 0){throw 'APK signature verification failed'}
Write-Host ('Built Android APK: '+$apk)
