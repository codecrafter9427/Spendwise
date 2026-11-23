param(
    [string]$Container = "spendwise-postgres",
    [string]$OutDir = ".\backups"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$stamp = Get-Date -Format "yyyyMMddHHmmss"
$remote = "/tmp/spendwise-$stamp.dump"
$local = Join-Path $OutDir "spendwise-$stamp.dump"

docker exec $Container pg_dump -U spendwise -d spendwise -Fc -f $remote
docker cp "${Container}:${remote}" $local
docker exec $Container rm -f $remote
Write-Host "Wrote $local"
