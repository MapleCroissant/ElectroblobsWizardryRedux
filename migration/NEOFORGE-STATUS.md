# NeoForge 1.21.1 migration status

Implemented locally on `neoforge-1.21.1`, based on Fabric commit `d05e6760`.
No branch switches, commits, destructive Git operations or pushes were performed.
The legacy `forge/` implementation is untouched. `common/`, `fabric/` and
`neoforge/` are configured as independent shared/Fabric/NeoForge builds.

## Artifacts and dependencies

- NeoForge: `neoforge/build/libs/electroblobs-wizardry-redux-1.21.1-0.8.10-neoforge.jar`
- Fabric: `fabric/build/libs/electroblobs-wizardry-redux-1.21.1-0.8.10-fabric.jar`
- Required for NeoForge: Minecraft 1.21.1, Java 21, NeoForge **21.1.266**
  (metadata permits newer 21.1 releases).
- Tooling: Gradle 8.11, ModDevGradle 2.0.148, official Mojang names.
- MarkdownGenerator 1.3.1.1 is embedded. No Fabric API or CCA is needed by NeoForge.
- Optional tested APIs: JEI 19.21.1.304, Cloth Config 15.0.140,
  Curios 9.5.1+1.21.1, Accessories 1.1.0-beta.52+1.21.1.

## Implementation

Native lifecycle/registry events register the existing gameplay content and
custom spell/element/tier registries. Native attachments persist wizard data,
spell discovery, command casting, minions, containment and arcane locks, with
player cloning and tracking synchronization. Items retain vanilla components
and the Fabric port's persistence/data-fixer behavior. Legacy entity/block
CCA/Forge fields are imported when native attachment data is absent.

Typed payloads retain the common channels and logic. Command registration and
advancement triggers defer to NeoForge lifecycle events; magic effects sync
after spawn bundles. Client registration includes renderers, layers, screens,
HUD/key mappings, armor models/textures and particles. Loader-specific durability
and attribute hooks preserve armor mana and conjured-item protection.

Curios and Accessories retain optional loading and the hotbar fallback. Curios
player slot assignments and live equip-state queries are provided. JEI has altar
and workbench categories, weighted outputs, spell-binding/tier-upgrade displays,
scroll information and spell-aware item subtypes. Pending glyph synchronization
no longer causes recipe-viewer ingredient-name failures.

Biome modifier resources, loot injections, tags, commands and all converted
structure templates are retained. Data generation follows the Fabric baseline:
regenerate native spell defaults and export canonical JSON for review, rather
than overwriting curated resources. The original Forge model/recipe provider
classes were not all rewritten as separate native provider classes.

## Validation

- Fresh Fabric baseline: compilation and all **36** required tests passed.
- Shared Java compilation and both loader builds passed.
- NeoForge: all **42** required tests passed with Curios and with Accessories,
  including actual equip/unequip detection.
- All **42** tests also passed from the packaged NeoForge JAR with optional mods
  absent. That run uses the JAR in an isolated `mods/` directory, not development
  mod source loading, and exercises the embedded Markdown library.
- Coverage includes the original 36 regression cases plus native entity/player
  attachments, legacy import precedence, native armor attributes, equipment and elemental biome tag coverage.
- Fabric compilation and all **36** required tests passed after shared changes.
- NeoForge data generation passed, exporting **1,250** JSON resources.
- Regular dedicated-server startup, world generation, 100 ticks and clean
  shutdown passed. The actual packaged JAR passed the same standalone startup.
- Client resource initialization, integrated-world login, JEI recipe startup,
  configuration synchronization, 200 client ticks and clean shutdown passed.
  The packaged client also completed integrated-world login and 200 ticks without optional mods.

Runtime inventories match across **28** compared registries, including **125**
spell registry entries, **352** items and **46** entities. These are measured
Fabric-baseline counts, including placeholders, rather than promotional counts.
No registered baseline content was removed. All **2,541** top-level Fabric
asset/data resources are present byte-for-byte in NeoForge; its five extra
resources define biome modifiers and Curios slots. See `loader-comparison.json`
and the two `*-1.21.1-content.json` snapshots.

Evidence: `neoforge-build-validation.log`, `neoforge-curios-server-validation.log`,
`neoforge-gametest-curios.log`, `neoforge-gametest-accessories.log`, `neoforge-client-smoke.log`,
`neoforge-packaged-smoke.log`, `neoforge-packaged-final.log`.

## Commands

```powershell
.\gradlew.bat :common:compileJava :neoforge:build :fabric:build
.\gradlew.bat :neoforge:runGameTestServer
.\gradlew.bat :neoforge:runGameTestServer -PwithCurios
.\gradlew.bat :neoforge:runGameTestServer -PwithAccessories
.\gradlew.bat :neoforge:runGameTestServer -PwithoutOptionalIntegrations
.\gradlew.bat :neoforge:runData
.\gradlew.bat :fabric:runGameTest
.\gradlew.bat :neoforge:runClient
.\gradlew.bat :neoforge:runServer
.\gradlew.bat :neoforge:runClientSmoke
.\gradlew.bat :neoforge:runServerSmoke
.\gradlew.bat :neoforge:runServerJarSmoke -PwithoutOptionalIntegrations
.\gradlew.bat :neoforge:runGameTestJarSmoke -PwithoutOptionalIntegrations
.\gradlew.bat :neoforge:runClientJarSmoke -PwithoutOptionalIntegrations
powershell -NoProfile -ExecutionPolicy Bypass -File migration/Compare-Loaders.ps1
```

Smoke profiles use isolated generated test worlds. Dedicated smoke profiles bind
only to localhost on an ephemeral port and stop automatically; normal client and
server profiles retain normal gameplay behavior. Generated outputs stay local.

## Remaining manual verification

No exhaustive visual playthrough of every spell or multi-client dedicated-server
session was performed. Check spell particles/beams, custom armor appearance,
GUI interactions/JEI layouts, natural structure placement, tracking across
multiple players, player death/respawn/dimension travel, and upgrades of backed-up
real worlds. Automated attachment save/load and optional equipment checks are
not a guarantee for every historical modpack/world combination.

Existing resource warnings remain: missing subtitle translations, a few missing
sound entries and the vanilla shader sampler warning. Development logs also
contain environment/authentication-service and optional annotation/deprecation
warnings. These are not hidden or resolved by deleting features/resources.
