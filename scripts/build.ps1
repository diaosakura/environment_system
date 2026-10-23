param([switch]$SkipWeb)
. (Join-Path $PSScriptRoot 'env.ps1')
Push-Location $ProjectRoot
try {
    if (-not $SkipWeb) {
        Push-Location (Join-Path $ProjectRoot 'web')
        try {
            if (-not (Test-Path -LiteralPath 'node_modules')) {
                & npm.cmd ci --no-audit --no-fund
                if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
            }
            & npm.cmd run build
            if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
        } finally { Pop-Location }
        $staticDir = Join-Path $ProjectRoot 'src\main\resources\static'
        $resolvedStatic = [IO.Path]::GetFullPath($staticDir)
        if (-not $resolvedStatic.StartsWith([IO.Path]::GetFullPath($ProjectRoot) + '\',[StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid static path' }
        New-Item -ItemType Directory -Path $staticDir -Force | Out-Null
        Get-ChildItem -LiteralPath $staticDir -Force | Where-Object Name -ne '.gitkeep' | Remove-Item -Recurse -Force
        Copy-Item -Path (Join-Path $ProjectRoot 'web\dist\*') -Destination $staticDir -Recurse -Force
    }
    & (Join-Path $ProjectRoot 'mvnw.cmd') '-Dmaven.repo.local=.m2' clean package
    if ($LASTEXITCODE -ne 0) { throw 'Maven tests or package failed' }
    Write-Host 'Built: target/environment_system-1.0.0.jar'
} finally { Pop-Location }
