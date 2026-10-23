. (Join-Path $PSScriptRoot 'env.ps1')
if (-not (Test-Path -LiteralPath $MysqldExe)) { throw 'MySQL runtime missing. Run scripts/bootstrap.ps1 or set MYSQL_HOME.' }
$dataDir = Join-Path $ProjectRoot 'data\mysql'
$configPath = Join-Path $ProjectRoot 'data\mysql.ini'
$serverPidPath = Join-Path $ProjectRoot 'data\mysql.pid'
New-Item -ItemType Directory -Path $dataDir -Force | Out-Null
$env:MYSQL_PWD = $env:DB_PASSWORD
function Test-MysqlReady {
    $pingInfo = New-Object Diagnostics.ProcessStartInfo
    $pingInfo.FileName = $MysqlExe
    $pingInfo.Arguments = '--protocol=TCP --host=127.0.0.1 --port=' + $env:DB_PORT + ' --user=' + $env:DB_USER + ' --connect-timeout=2 --execute="SELECT 1"'
    $pingInfo.UseShellExecute = $false
    $pingInfo.RedirectStandardError = $true
    $pingInfo.RedirectStandardOutput = $true
    $pingProcess = New-Object Diagnostics.Process
    $pingProcess.StartInfo = $pingInfo
    $pingProcess.Start() | Out-Null
    $pingProcess.WaitForExit()
    $ok = $pingProcess.ExitCode -eq 0
    $pingProcess.Dispose()
    return $ok
}
if (Test-MysqlReady) { Write-Host 'MySQL is already available.'; exit 0 }
$tcp = New-Object Net.Sockets.TcpClient
try { $tcp.Connect('127.0.0.1',[int]$env:DB_PORT); throw 'Port is occupied but database credentials failed. Check existing service; no changes were made to it.' }
catch [Net.Sockets.SocketException] {}
finally { $tcp.Dispose() }
$mysqlAccessRoot = $ProjectRoot
if ($ProjectRoot -match '[^\x00-\x7F]') {
    $pathHasher = [Security.Cryptography.SHA256]::Create()
    try { $pathDigest = [BitConverter]::ToString($pathHasher.ComputeHash([Text.Encoding]::UTF8.GetBytes($ProjectRoot))).Replace('-','').Substring(0,12) }
    finally { $pathHasher.Dispose() }
    $mysqlAccessRoot = Join-Path $env:TEMP ('environment-system-' + $pathDigest)
    if ($mysqlAccessRoot -match '[^\x00-\x7F]') { throw 'MySQL needs an ASCII access path. Move the project to an English-only directory, such as C:\Projects\environment_system.' }
    if (Test-Path -LiteralPath $mysqlAccessRoot) {
        $linkItem = Get-Item -LiteralPath $mysqlAccessRoot -Force
        if ($linkItem.LinkType -ne 'Junction' -or $linkItem.Target -ne $ProjectRoot) { throw 'Runtime alias already exists with a different target.' }
    } else { New-Item -ItemType Junction -Path $mysqlAccessRoot -Target $ProjectRoot | Out-Null }
}
[IO.File]::WriteAllText((Join-Path $ProjectRoot 'data\mysql-alias.txt'),$mysqlAccessRoot)
$nativeMysqlHome = $MysqlHome
if ($MysqlHome.StartsWith($ProjectRoot + '\',[StringComparison]::OrdinalIgnoreCase)) {
    $nativeMysqlHome = $mysqlAccessRoot + $MysqlHome.Substring($ProjectRoot.Length)
}
$nativeMysqld = Join-Path $nativeMysqlHome 'bin\mysqld.exe'
$configPath = Join-Path $mysqlAccessRoot 'data\mysql.ini'
$baseSlash = $nativeMysqlHome.Replace('\','/')
$dataSlash = (Join-Path $mysqlAccessRoot 'data\mysql').Replace('\','/')
$pidSlash = (Join-Path $mysqlAccessRoot 'data\mysql.pid').Replace('\','/')
$logSlash = (Join-Path $mysqlAccessRoot 'logs\mysql-error.log').Replace('\','/')
$configuration = @"
[mysqld]
basedir="$baseSlash"
datadir="$dataSlash"
port=$env:DB_PORT
bind-address=127.0.0.1
mysqlx=0
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
max_connections=30
innodb_buffer_pool_size=128M
log-error="$logSlash"
pid-file="$pidSlash"
"@
[IO.File]::WriteAllText($configPath,$configuration,(New-Object Text.UTF8Encoding($false)))
$fresh = -not (Test-Path -LiteralPath (Join-Path $dataDir 'mysql'))
if ($fresh) {
    & $nativeMysqld "--defaults-file=$configPath" --initialize-insecure
    if ($LASTEXITCODE -ne 0) { throw 'MySQL initialization failed. See logs/mysql-error.log; VC++ 2015-2022 x64 runtime may be required.' }
}
$mysqlProcess = Start-Process -FilePath $nativeMysqld -ArgumentList ('"--defaults-file=' + $configPath + '"') -PassThru -WindowStyle Hidden
$ready = $false
for ($attempt=0; $attempt -lt 45; $attempt++) {
    Start-Sleep -Seconds 1
    if ($mysqlProcess.HasExited) { throw 'MySQL exited. See logs/mysql-error.log.' }
    if ($fresh) { $env:MYSQL_PWD = '' }
    if (Test-MysqlReady) { $ready=$true; break }
}
if (-not $ready) { throw 'MySQL startup timed out. See logs/mysql-error.log.' }
if ($fresh) {
    & $MysqlExe --protocol=TCP --host=127.0.0.1 "--port=$env:DB_PORT" --user=root --skip-password --execute="ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot set demo root password.' }
}
$env:MYSQL_PWD = $env:DB_PASSWORD
Write-Host "MySQL ready on localhost:$env:DB_PORT (project-owned data directory)."
