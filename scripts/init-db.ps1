. (Join-Path $PSScriptRoot 'env.ps1')
if ($env:DB_NAME -notmatch '^[a-zA-Z0-9_]+$') { throw 'DB_NAME must contain only letters, digits, underscores.' }
if (-not (Test-Path -LiteralPath $MysqlExe)) { throw 'mysql.exe missing. Set MYSQL_HOME or run bootstrap.ps1.' }
$env:MYSQL_PWD=$env:DB_PASSWORD
$schema=[IO.File]::ReadAllText((Join-Path $ProjectRoot 'sql\schema.sql'),[Text.Encoding]::UTF8).Replace('environment_system',$env:DB_NAME)
$psi=New-Object Diagnostics.ProcessStartInfo
$psi.FileName=$MysqlExe
$psi.Arguments="--protocol=TCP --host=$env:DB_HOST --port=$env:DB_PORT --user=$env:DB_USER --default-character-set=utf8mb4"
$psi.UseShellExecute=$false
$psi.RedirectStandardInput=$true
$process=New-Object Diagnostics.Process
$process.StartInfo=$psi
$process.Start() | Out-Null
$sqlBytes=[Text.Encoding]::UTF8.GetBytes($schema)
$process.StandardInput.BaseStream.Write($sqlBytes,0,$sqlBytes.Length)
$process.StandardInput.BaseStream.Flush()
$process.StandardInput.Close()
$process.WaitForExit()
if ($process.ExitCode -ne 0) { throw 'Schema creation failed. Existing data was not dropped.' }
Push-Location $ProjectRoot
try {
    $seedJar=Join-Path $ProjectRoot 'target\environment_system-1.0.0.jar'
    if (-not (Test-Path -LiteralPath $seedJar)) {
        & (Join-Path $ProjectRoot 'mvnw.cmd') '-Dmaven.repo.local=.m2' -B -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'Backend packaging failed' }
    }
    & (Join-Path $env:JAVA_HOME 'bin\java.exe') '-Dfile.encoding=UTF-8' -jar $seedJar --app.seed=true --spring.main.web-application-type=none
    if ($LASTEXITCODE -ne 0) { throw 'Demo seeding failed. Existing initialized database will not be overwritten.' }
} finally { Pop-Location }
