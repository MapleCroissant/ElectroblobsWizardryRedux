param(
    [string]$FabricInventory = (Join-Path $PSScriptRoot 'fabric-1.21.1-content.json'),
    [string]$NeoForgeInventory = (Join-Path $PSScriptRoot 'neoforge-1.21.1-content.json'),
    [string]$FabricJar = (Join-Path $PSScriptRoot '../fabric/build/libs/electroblobs-wizardry-redux-1.21.1-0.8.10-fabric.jar'),
    [string]$NeoForgeJar = (Join-Path $PSScriptRoot '../neoforge/build/libs/electroblobs-wizardry-redux-1.21.1-0.8.10-neoforge.jar')
)
$ErrorActionPreference = 'Stop'
$fabric = Get-Content -Raw -LiteralPath $FabricInventory | ConvertFrom-Json
$neo = Get-Content -Raw -LiteralPath $NeoForgeInventory | ConvertFrom-Json
$registryDifferences = @()
foreach ($property in $fabric.PSObject.Properties) {
    $other = $neo.PSObject.Properties[$property.Name]
    if (-not $other) { $registryDifferences += "Missing registry $($property.Name)" }
    elseif (Compare-Object @($property.Value) @($other.Value)) { $registryDifferences += $property.Name }
}
foreach ($property in $neo.PSObject.Properties) {
    if (-not $fabric.PSObject.Properties[$property.Name]) { $registryDifferences += "Extra registry $($property.Name)" }
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Get-ResourceHashes([string]$Jar) {
    $zip = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Jar))
    $hashes = @{}
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName -notmatch '^(assets|data)/' -or $entry.FullName.EndsWith('/')) { continue }
            $stream = $entry.Open()
            $sha = [Security.Cryptography.SHA256]::Create()
            try { $hashes[$entry.FullName] = [BitConverter]::ToString($sha.ComputeHash($stream)) }
            finally { $stream.Dispose(); $sha.Dispose() }
        }
    } finally { $zip.Dispose() }
    return $hashes
}
$fabricResources = Get-ResourceHashes $FabricJar
$neoResources = Get-ResourceHashes $NeoForgeJar
$missing = @(); $changed = @()
foreach ($path in $fabricResources.Keys) {
    if (-not $neoResources.ContainsKey($path)) { $missing += $path }
    elseif ($fabricResources[$path] -ne $neoResources[$path]) { $changed += $path }
}
$result = [pscustomobject]@{
    ComparedRegistries = @($fabric.PSObject.Properties).Count
    RegistryDifferences = $registryDifferences
    Spells = $fabric.'ebwizardry:spells'.Count
    Items = $fabric.'minecraft:item'.Count
    Entities = $fabric.'minecraft:entity_type'.Count
    FabricResources = $fabricResources.Count
    NeoForgeResources = $neoResources.Count
    MissingFabricResources = $missing
    ChangedFabricResources = $changed
    ExtraNeoForgeResources = @($neoResources.Keys | Where-Object { -not $fabricResources.ContainsKey($_) } | Sort-Object)
}
$json = $result | ConvertTo-Json -Depth 5
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'loader-comparison.json'), $json)
$json
if ($registryDifferences.Count -or $missing.Count -or $changed.Count) { exit 1 }
