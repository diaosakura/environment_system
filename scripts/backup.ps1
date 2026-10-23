. (Join-Path $PSScriptRoot 'env.ps1')
$dumpExe=Join-Path $MysqlHome 'bin\mysqldump.exe'
$backupDir=Join-Path $ProjectRoot 'backups'
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
$backupFile=Join-Path $backupDir ($env:DB_NAME+'-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.sql')
$env:MYSQL_PWD=$env:DB_PASSWORD
& $dumpExe --protocol=TCP "--host=$env:DB_HOST" "--port=$env:DB_PORT" "--user=$env:DB_USER" --default-character-set=utf8mb4 --single-transaction --no-tablespaces --set-gtid-purged=OFF "--result-file=$backupFile" $env:DB_NAME
if ($LASTEXITCODE -ne 0) { throw 'Database backup failed' }
Write-Host "Backup saved: $backupFile"
