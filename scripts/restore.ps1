param([Parameter(Mandatory=$true)][string]$BackupFile,[switch]$ConfirmRestore)
. (Join-Path $PSScriptRoot 'env.ps1')
if (-not $ConfirmRestore) { throw 'Restore replaces table data. Back up first, then rerun with -ConfirmRestore.' }
if (-not (Test-Path -LiteralPath $BackupFile -PathType Leaf)) { throw 'Backup SQL file not found' }
if ($env:DB_NAME -notmatch '^[a-zA-Z0-9_]+$') { throw 'Invalid DB_NAME' }
$env:MYSQL_PWD=$env:DB_PASSWORD
& $MysqlExe --protocol=TCP "--host=$env:DB_HOST" "--port=$env:DB_PORT" "--user=$env:DB_USER" "--execute=CREATE DATABASE IF NOT EXISTS $env:DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
if ($LASTEXITCODE -ne 0) { throw 'Cannot create database' }
$psi=New-Object Diagnostics.ProcessStartInfo
$psi.FileName=$MysqlExe
$psi.Arguments="--protocol=TCP --host=$env:DB_HOST --port=$env:DB_PORT --user=$env:DB_USER --default-character-set=utf8mb4 $env:DB_NAME"
$psi.UseShellExecute=$false
$psi.RedirectStandardInput=$true
$process=New-Object Diagnostics.Process
$process.StartInfo=$psi
$process.Start() | Out-Null
$sqlBytes=[Text.Encoding]::UTF8.GetBytes([IO.File]::ReadAllText((Resolve-Path -LiteralPath $BackupFile),[Text.Encoding]::UTF8))
$process.StandardInput.BaseStream.Write($sqlBytes,0,$sqlBytes.Length)
$process.StandardInput.BaseStream.Flush()
$process.StandardInput.Close()
$process.WaitForExit()
if ($process.ExitCode -ne 0) { throw 'Restore failed' }
Write-Host 'Database restored. Do not run init-db again.'
