param([string]$Manifest)
$ErrorActionPreference='Stop'
$projectDir=Split-Path -Parent $PSScriptRoot
if(-not $Manifest){$Manifest=Join-Path $projectDir 'release\CONTENTS-SHA256SUMS.txt'}
if(-not(Test-Path -LiteralPath $Manifest)){throw 'Package manifest not found. Use the exported full-source ZIP.'}
$checked=0
foreach($line in Get-Content -LiteralPath $Manifest -Encoding UTF8){
    if(-not $line.Trim()){continue}
    if($line -notmatch '^([a-fA-F0-9]{64})  (.+)$'){throw 'Invalid manifest row'}
    $expected=$Matches[1];$relative=$Matches[2]
    $candidate=[IO.Path]::GetFullPath((Join-Path $projectDir $relative))
    if(-not $candidate.StartsWith([IO.Path]::GetFullPath($projectDir)+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Manifest path escapes the project'}
    if(-not(Test-Path -LiteralPath $candidate -PathType Leaf)){throw ('Missing: '+$relative)}
    if((Get-FileHash -LiteralPath $candidate -Algorithm SHA256).Hash -ne $expected){throw ('Checksum mismatch: '+$relative)}
    $checked++
}
Write-Host ('PASS: '+$checked+' package files match SHA256. Generated data, caches and local settings are not part of this manifest.')
