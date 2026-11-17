# Fabric 1.21.1 validation

Branch: `fabric-1.21.1`. Baseline: `ff6b02f7b5a467493f7e779efa0df97f74079b2e`.

## Passed

- Original 1.20.1 Fabric compile and all 24 original required GameTests.
- Java 21 shared compilation (`:common:compileJava`).
- Fabric build, sources/Javadoc artifacts, remapping and access-widener checks.
  Gradle has no Fabric JUnit sources; `build` alone does not run GameTests.
- All 36 required Fabric GameTests with Trinkets 3.10.0.
- All 36 required Fabric GameTests with Accessories 1.1.0-beta.52+1.21.1
  and without Trinkets.
- All 36 required Fabric GameTests without either accessory mod.
- Fabric data generation: 1,245 canonical JSON files exported/regenerated.
- Final development client resource/renderer initialization and clean shutdown
  (`:fabric:runClient`, `BUILD SUCCESSFUL`). See `client-smoke-1.21.1.log`.
- Baseline inventory comparison: 2,535 shared resources accounted for after path
  migration; no missing original registry strings in the 35 inventoried sources.
- All 30 structure templates load through the runtime structure manager.
  Offline native conversion preserved each template's block and entity counts.
- Regression coverage includes wand selection/progression/cooldowns and copying,
  armor mana, conjured item expiry/persistence, temporary enchantments,
  conditional modifier NBT, every registered entity's save/load path, altar recipe
  JSON/network serialization, weighted book-repair output data, magic effect
  packet serialization, and legacy custom item/wizard-trade conversion.

Logs: `baseline-gametest.log`, `gametest-1.21.1-passed.log`,
`gametest-accessories.log`, `gametest-vanilla-artifacts.log`, `build-final.log`.

## Artifact inspection

`fabric/build/libs/electroblobs-wizardry-redux-1.21.1-0.8.10-fabric.jar`

- Java class major version 65 (Java 21).
- Metadata requires Minecraft 1.21.1, Java 21, Fabric Loader and Fabric API.
- CCA base/block/entity and MarkdownGenerator are bundled.
- Mixin refmap and all 30 structure templates are packaged.
- No old plural native data-registry resource paths or offline conversion tool.
- SHA-256: `D16C9352A2F4890F0DA26334E6EDF5DB17B631313C9B384DC7CAAA550740E5B5`.

## Scope and remaining checks

These are development-environment checks. A separate installed-modpack smoke
test of the remapped artifact is still recommended. The client has previously
reached an integrated-world login and completed config/spell synchronization.
The initial world-generation test exposed legacy structure conversion failure;
the converted templates now pass the runtime structure-loading regression.

No exhaustive visual or multiplayer playthrough was performed. Verify all spell
visuals, GUI interactions, artifact equip/unequip behavior, packet tracking across
multiple players, natural structure placement, and upgrades of backed-up worlds.
Optional integration startup passing is not exhaustive slot/effect testing.

Known logs include pre-existing missing custom entity data-fixer messages,
missing sounds and duplicate JEI book/scroll subtypes. These are not suppressed.
Some deprecated API and optional annotation compiler warnings remain.

No Forge implementation files were changed. Forge is excluded from this branch's
build because its retained implementation still targets 1.20.1. No GitHub push
was performed.
