param([switch]$MySql)
. (Join-Path $PSScriptRoot 'env.ps1')
$pidFile=Join-Path $ProjectRoot 'data\app.pid'
if (Test-Path -LiteralPath $pidFile) {
    $appId=[int](Get-Content -LiteralPath $pidFile -Raw)
    $appInfo=Get-CimInstance Win32_Process -Filter "ProcessId=$appId" -ErrorAction SilentlyContinue
    if ($appInfo -and $appInfo.CommandLine -like ('*'+(Join-Path $ProjectRoot 'target\environment_system-1.0.0.jar')+'*')) {
        Stop-Process -Id $appId
        Write-Host 'Application stopped.'
    } elseif ($appInfo) { throw 'PID belongs to another process; refused to stop it.' }
    Remove-Item -LiteralPath $pidFile -Force
}
if ($MySql) {
    $mysqlPidFile=Join-Path $ProjectRoot 'data\mysql.pid'
    if (-not (Test-Path -LiteralPath $mysqlPidFile)) { throw 'No project-owned MySQL PID file; will not stop an external database.' }
    $mysqlId=[int](Get-Content -LiteralPath $mysqlPidFile -Raw)
    $mysqlInfo=Get-CimInstance Win32_Process -Filter "ProcessId=$mysqlId" -ErrorAction SilentlyContinue
    $expectedConfig=Join-Path $ProjectRoot 'data\mysql.ini'
    $aliasFile=Join-Path $ProjectRoot 'data\mysql-alias.txt'
    if (Test-Path -LiteralPath $aliasFile) { $expectedConfig=Join-Path (Get-Content -LiteralPath $aliasFile -Raw) 'data\mysql.ini' }
    if (-not $mysqlInfo -or $mysqlInfo.CommandLine -notlike ('*'+$expectedConfig+'*')) { throw 'Cannot verify project-owned MySQL process.' }
    $env:MYSQL_PWD=$env:DB_PASSWORD
    & $MysqlAdmin --protocol=TCP "--host=$env:DB_HOST" "--port=$env:DB_PORT" "--user=$env:DB_USER" shutdown
    if ($LASTEXITCODE -ne 0) { throw 'MySQL shutdown failed' }
    Write-Host 'Project MySQL stopped.'
}
