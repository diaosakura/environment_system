. (Join-Path $PSScriptRoot 'env.ps1')
$jar=Join-Path $ProjectRoot 'target\environment_system-1.0.0.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Build first: scripts/build.ps1' }
$pidFile=Join-Path $ProjectRoot 'data\app.pid'
New-Item -ItemType Directory -Path (Join-Path $ProjectRoot 'data') -Force | Out-Null
if (Test-Path -LiteralPath $pidFile) {
    $oldProcess=Get-Process -Id ([int](Get-Content -LiteralPath $pidFile -Raw)) -ErrorAction SilentlyContinue
    if ($oldProcess) { throw 'Recorded application process is still running. Stop it before starting again.' }
}
$appProcess=Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin\java.exe') -ArgumentList ('-Dfile.encoding=UTF-8 -jar "'+$jar+'"') -WorkingDirectory $ProjectRoot -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $ProjectRoot 'logs\app-stdout.log') -RedirectStandardError (Join-Path $ProjectRoot 'logs\app-stderr.log')
[IO.File]::WriteAllText($pidFile,$appProcess.Id.ToString())
Write-Host "Application starting (PID $($appProcess.Id)). Open http://localhost:$env:SERVER_PORT after /api/health reports UP."
