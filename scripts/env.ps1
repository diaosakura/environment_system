$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$RuntimeRoot = Join-Path $ProjectRoot '.runtime'
if (Test-Path -LiteralPath (Join-Path $RuntimeRoot 'jdk-17.0.9\bin\java.exe')) {
    $env:JAVA_HOME = Join-Path $RuntimeRoot 'jdk-17.0.9'
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    throw 'JDK not found. Run scripts/bootstrap.ps1 or set JAVA_HOME to JDK 17.0.9.'
}
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH
$MysqlHome = Join-Path $RuntimeRoot 'mysql-8.4.11-winx64'
if ($env:MYSQL_HOME) { $MysqlHome = $env:MYSQL_HOME }
$MysqlExe = Join-Path $MysqlHome 'bin\mysql.exe'
$MysqlAdmin = Join-Path $MysqlHome 'bin\mysqladmin.exe'
$MysqldExe = Join-Path $MysqlHome 'bin\mysqld.exe'
if (-not $env:DB_HOST) { $env:DB_HOST = 'localhost' }
if (-not $env:DB_PORT) { $env:DB_PORT = '3306' }
if (-not $env:DB_NAME) { $env:DB_NAME = 'environment_system' }
if (-not $env:DB_USER) { $env:DB_USER = 'root' }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = 'root' }
if (-not $env:SERVER_PORT) { $env:SERVER_PORT = '8080' }
New-Item -ItemType Directory -Path (Join-Path $ProjectRoot 'logs') -Force | Out-Null
