# NeoForge 1.21.1 migration

Verified before edits: branch `neoforge-1.21.1` at `d05e6760`, matching the
completed Fabric branch. Existing untracked validation logs are retained.

1. Re-run Fabric compile/GameTests and retain the Fabric preservation inventory.
2. Add an independent NeoForge ModDevGradle module with Java 21, official
   mappings, loader metadata, shared source/resource inputs and optional deps.
3. Adapt legacy Forge registry/lifecycle behavior to NeoForge; preserve IDs.
4. Implement native attachments, persistence, cloning and tracking sync; retain
   native item components from the working Fabric port. Register typed payloads.
5. Adapt client registration, events, rendering, armor/mana mixins and config.
6. Migrate worldgen resources and data generation. Add NeoForge JEI/Curios
   integration while retaining optional loading and vanilla artifact behavior.
7. Compile incrementally; reuse the Fabric regression cases and investigate
   failures rather than bypassing functionality.
8. Exercise data generation, server/client startup, optional integrations, and
   content inventories; rerun Fabric checks after shared changes.
9. Build an installable NeoForge JAR and record exact validation/limitations.

Main compatibility boundaries: deferred registry ordering and custom registries;
Forge capabilities versus NeoForge attachments; thread-safe typed networking;
physical client isolation; patched NeoForge item/equipment hooks; optional API
loading; native tags/biome modifiers and loot injection. Common gameplay remains
the primary implementation. Legacy Forge stays intact as a behavior reference.

Reference tooling: the official NeoForge 1.21.1 ModDevGradle MDK, initially
NeoForge 21.1.266 and ModDevGradle 2.0.148. Dependency resolution and runtime
checks determine whether these pinned versions work with this repository.
