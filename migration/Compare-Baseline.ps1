param([string]$Repository = (Split-Path -Parent $PSScriptRoot))

$ErrorActionPreference = 'Stop'
$baseline = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'baseline-1.20.1.json') -Raw | ConvertFrom-Json
$missing = @()
$changed = @()
foreach ($resource in $baseline.resources) {
    $migrated = $resource.path -replace '(/data/[^/]+/)advancements/', '${1}advancement/' -replace '(/data/[^/]+/)loot_tables/', '${1}loot_table/' -replace '(/data/[^/]+/)recipes/', '${1}recipe/' -replace '(/data/[^/]+/)structures/', '${1}structure/'
    $migrated = $migrated -replace '/gametest/structures/', '/gametest/structure/'
    $migrated = $migrated -replace '/tags/items/', '/tags/item/' -replace '/tags/blocks/', '/tags/block/'
    $path = Join-Path $Repository $migrated
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $missing += $resource.path
    } elseif ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ne $resource.sha256) {
        $changed += $resource.path
    }
}

# IDs are checked separately from file hashes because Java API migrations change
# registry source code while the original registration names must remain stable.
$missingIds = @()
foreach ($registry in $baseline.registrySources) {
    $path = Join-Path $Repository $registry.path
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $missingIds += "Missing registry source: $($registry.path)"
        continue
    }
    $current = [System.IO.File]::ReadAllText($path)
    $originalStrings = [regex]::Matches($registry.source, '"[a-z0-9_./:-]+"') |
        ForEach-Object { $_.Value } | Sort-Object -Unique
    foreach ($id in $originalStrings) {
        if (-not $current.Contains($id)) {
            $missingIds += "$($registry.path): $id"
        }
    }
}

[pscustomobject]@{
    BaselineCommit = $baseline.commit
    ResourceCount = $baseline.resources.Count
    MissingResources = $missing
    ChangedResources = $changed
    RegistryStringsRequiringReview = $missingIds
} | ConvertTo-Json -Depth 4

# Missing paths can be intentional 1.21 directory renames; report them for review.
# This inventory check does not claim runtime feature parity.
