# Fabric 1.21.1 migration

Work is local to `fabric-1.21.1`. No publishing or pushing is authorized.

## Preservation baseline

`baseline-1.20.1.json` records the starting commit, all 35 registry source
files (including their complete source), and SHA-256 hashes of 2,535 shared
resources. The inventory excludes generated documentation and datagen caches.
Use it to review registry IDs and resource coverage throughout the port.

## Acceptance criteria

- Java 21, Minecraft 1.21.1, and a remapped Fabric JAR.
- Preserve every existing registered spell, item, wand, ability, entity, and
  gameplay mechanic; do not exclude sources or disable injections to compile.
- Preserve registry IDs; explicitly document any necessary representation changes.
- Compile incrementally, run Fabric GameTests, and check client and dedicated
  server startup, persistence, networking, rendering, and optional integrations.
- Keep Forge implementation changes outside the scope of this port.

## Stages

1. Capture the existing Fabric compile/GameTest result and preservation inventory.
2. Configure Fabric/shared compilation for Java 21 and Minecraft 1.21.1.
3. Migrate data components, CCA serialization, and conditional attributes.
4. Migrate networking, recipes, registries, and gameplay APIs.
5. Migrate rendering and every configured mixin.
6. Migrate resources and Fabric data generation.
7. Run regression tests, compare inventories, and build the remapped JAR.

## Initial environment

- Clean starting branch: `fabric-1.21.1`.
- Installed Java: Temurin 21.0.8.
- Baseline command: `gradlew.bat :fabric:compileJava :fabric:runGameTest --console=plain`.
- The sandbox could not create the Gradle cache lock at `C:\.gradle`; the
  command was retried through the environment's approval mechanism.
- Baseline dependency resolution selected Loom 1.9.2 from `1.9-SNAPSHOT`.

This directory records the baseline, migration scripts, and validation results.

## Baseline result

The original 1.20.1 configuration compiled successfully and all 24 required
Fabric GameTests passed under Java 21 (4m 25s). See `baseline-gametest.log`.
Existing warnings included missing annotation classes, deprecated APIs,
duplicate JEI inputs during remapping, a client-only ItemProperties accessor
listed among server mixins, and missing entity data fixers.

## Build configuration decisions

- Pin Loom 1.9.2 rather than the previous snapshot selector; keep Gradle 8.11.
- Configure both common and Fabric with Loom and official Mojang mappings.
  Parchment's old 1.20.1 overlay is no longer applied.
- Exclude the preserved 1.20.1 Forge project from this Fabric-only branch's
  settings; do not rewrite Forge implementation code.
- Replace CCA's removed item dependency with vanilla components during the
  persistence stage. Item functionality is still required, not optional.
- Compilation logs are diagnostic snapshots, not evidence of a completed port.

## Implemented migration

The Fabric/shared sources use Java 21, native item components, registry holders,
typed payloads, registry-aware recipes and persistence, and updated rendering,
entity, interaction, advancement, loot and mixin APIs. Original spell and item
registration strings are checked against the baseline inventory.

Dependencies: Fabric Loader 0.16.14, Fabric API 0.116.16+1.21.1, Cardinal Components
6.1.3 (base/entity/block bundled), Trinkets 3.10.0, Accessories
1.1.0-beta.52+1.21.1, Cloth Config 15.0.140 and JEI 19.21.1.304.
Fabric API is required separately; accessory integrations remain optional.

Resource paths and native JSON schemas were migrated by `Migrate-Resources.ps1`.
Thirty structure templates were converted with Minecraft's own data fixer in
the isolated `:fabric:migrateStructureTemplates` tooling source set. Their block
and entity counts were preserved; that tool is excluded from the mod JAR.

## Commands

```powershell
.\gradlew.bat :common:compileJava :fabric:build
.\gradlew.bat :fabric:runGameTest
.\gradlew.bat :fabric:runGameTest -PwithoutTrinkets
.\gradlew.bat :fabric:runGameTest -PwithoutTrinkets -PwithAccessories
.\gradlew.bat :fabric:runClient
.\gradlew.bat :fabric:runDatagen
powershell -NoProfile -ExecutionPolicy Bypass -File migration/Compare-Baseline.ps1
```

The Fabric data generator regenerates spell defaults and exports the canonical
migrated JSON into `fabric/build/generated-data` for review. It does not overwrite
curated resources or automatically add that export to the packaged resources.
The original Forge-specific model/recipe provider implementations are retained;
they have not all been rewritten as native Fabric provider classes.

## Remaining verification

The final build and all 36 required GameTests pass with Trinkets, Accessories,
and neither integration. Client resource initialization and clean shutdown also
pass. Full commands, artifact inspection and limitations are in
[`VALIDATION.md`](VALIDATION.md).

Automated tests cover selected gameplay and persistence paths, not every spell
or possible interaction. Visually verify spell particles, beams, overlays, armor,
mount attachments and GUI interaction. Exercise multiplayer casting and tracking,
artifact slots, world generation and backed-up 1.20.1 worlds before release.
The original game logs already contained missing custom entity data-fixer
warnings; runtime JEI and missing-sound warnings also need separate review.
See `BREAKING_CHANGES.md` for add-on/API migration notes.
