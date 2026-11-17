param([string]$Repository = (Split-Path -Parent $PSScriptRoot))
$ErrorActionPreference = 'Stop'
$Repository = [IO.Path]::GetFullPath($Repository)
$roots = @('common/src/main/resources/data', 'common/src/generated/resources/data', 'fabric/src/main/resources/data')
$renames = @{ advancements = 'advancement'; loot_tables = 'loot_table'; recipes = 'recipe'; structures = 'structure'; functions = 'function'; predicates = 'predicate'; item_modifiers = 'item_modifier'; blocks = 'block'; items = 'item'; entity_types = 'entity_type'; fluids = 'fluid'; game_events = 'game_event' }
foreach ($root in $roots) {
    # Only rename registry directories, not nested loot folders such as loot_table/blocks.
    $namespaces = Get-ChildItem -LiteralPath (Join-Path $Repository $root) -Directory
    foreach ($namespace in $namespaces) {
        $parents = @($namespace.FullName, (Join-Path $namespace.FullName 'tags'), (Join-Path $namespace.FullName 'gametest'))
        foreach ($parent in $parents) {
            if (-not (Test-Path -LiteralPath $parent)) { continue }
            foreach ($directory in @(Get-ChildItem -LiteralPath $parent -Directory)) {
                if (-not $renames.ContainsKey($directory.Name)) { continue }
                $source = [IO.Path]::GetFullPath($directory.FullName)
                $destination = [IO.Path]::GetFullPath((Join-Path $parent $renames[$directory.Name]))
                if (-not $source.StartsWith($Repository + '\', [StringComparison]::OrdinalIgnoreCase) -or
                    -not $destination.StartsWith($Repository + '\', [StringComparison]::OrdinalIgnoreCase)) {
                    throw "Resource rename escaped the repository: $source"
                }
                if (Test-Path -LiteralPath $destination) { throw "Rename destination exists: $destination" }
                Rename-Item -LiteralPath $source -NewName $renames[$directory.Name]
            }
        }
    }
}

function Put-Field($node, [string]$name, $value) {
    $node | Add-Member -MemberType NoteProperty -Name $name -Value $value -Force
    $script:changed = $true
}
function Convert-Node($node) {
    if ($null -eq $node -or $node -is [string] -or $node -is [ValueType]) { return }
    if ($node -is [Array]) { foreach ($child in $node) { Convert-Node $child }; return }
    if ($node -isnot [pscustomobject]) { return }
    $function = [string]$node.function
    if ($node.type -eq 'minecraft:loot_table' -and $node.name) {
        Put-Field $node 'value' $node.name
        $node.psobject.Properties.Remove('name')
    }
    if ($function -in @('looting_enchant', 'minecraft:looting_enchant')) {
        Put-Field $node 'function' 'minecraft:enchanted_count_increase'
        Put-Field $node 'enchantment' 'minecraft:looting'
    }
    if ($function -in @('set_nbt', 'minecraft:set_nbt')) { Put-Field $node 'function' 'minecraft:set_custom_data' }
    if ($node.condition -in @('random_chance_with_looting', 'minecraft:random_chance_with_looting')) {
        $chance = [double]$node.chance
        $bonus = [double]$node.looting_multiplier
        Put-Field $node 'condition' 'minecraft:random_chance_with_enchanted_bonus'
        Put-Field $node 'enchantment' 'minecraft:looting'
        Put-Field $node 'unenchanted_chance' $chance
        Put-Field $node 'enchanted_chance' ([pscustomobject]@{ type = 'minecraft:linear'; base = $chance + $bonus; per_level_above_first = $bonus })
        $node.psobject.Properties.Remove('chance')
        $node.psobject.Properties.Remove('looting_multiplier')
    }
    if ($node.enchantments -is [Array] -and $node.enchantments.Count -gt 0 -and $node.enchantments[0].enchantment) {
        foreach ($entry in $node.enchantments) {
            Put-Field $entry 'enchantments' $entry.enchantment
            $entry.psobject.Properties.Remove('enchantment')
        }
        Put-Field $node 'predicates' ([pscustomobject]@{ 'minecraft:enchantments' = $node.enchantments })
        $node.psobject.Properties.Remove('enchantments')
    }
    foreach ($property in @($node.psobject.Properties)) { Convert-Node $property.Value }
}

$updated = 0
foreach ($root in $roots) {
    foreach ($file in Get-ChildItem -LiteralPath (Join-Path $Repository $root) -Recurse -Filter '*.json' -File) {
        $json = [IO.File]::ReadAllText($file.FullName) | ConvertFrom-Json
        $script:changed = $false
        if ($file.FullName -match '\\recipe\\' -and $json.type -like 'minecraft:*' -and $json.result) {
            if ($json.result -is [string]) { Put-Field $json 'result' ([pscustomobject]@{ id = $json.result }) }
            elseif ($json.result.item) {
                Put-Field $json.result 'id' $json.result.item
                $json.result.psobject.Properties.Remove('item')
            }
        }
        if ($file.FullName -match '\\advancement\\' -and $json.display.icon.item) {
            Put-Field $json.display.icon 'id' $json.display.icon.item
            $json.display.icon.psobject.Properties.Remove('item')
            if ($json.display.icon.nbt -eq '{Damage:0}') {
                Put-Field $json.display.icon 'components' ([pscustomobject]@{ 'minecraft:damage' = 0 })
                $json.display.icon.psobject.Properties.Remove('nbt')
            }
        }
        Convert-Node $json
        if ($script:changed) {
            [IO.File]::WriteAllText($file.FullName, ($json | ConvertTo-Json -Depth 100) + "`n")
            $updated++
        }
    }
}
Write-Output "Migrated resource paths and updated $updated JSON files."
