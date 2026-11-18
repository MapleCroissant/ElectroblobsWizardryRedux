# Electroblob’s Wizardry Redux 1.21.1 NeoForge restoration progress

## Registry audit

The 1.12.2 reference at commit `c0be7475b234223e5a3276e62e38e69ffa6de0ce` has 189 spell definitions and 189 registration calls, including the `none` sentinel. Counts below compare the original definitions with the central Redux declarations and release filter, then cross-check the local packaged JAR. The matching counts measure casting implementation and release availability. They do not establish full mechanics parity.

| Scope | Entries | Basis |
| --- | ---: | --- |
| Original playable spells | 188 | Excludes `none` |
| Original spells with Redux casting implementations in source | 137 | 134 original IDs and 3 renamed equivalents. Includes both gated originals and partial Greater Telekinesis |
| Original spells available in the default Redux release registry | 135 | 133 original IDs and 2 release aliases. Includes partial Greater Telekinesis |
| Original spells unavailable in the default Redux release | 53 | 51 missing casting implementations and 2 development-gated implementations |
| All Redux source declarations | 142 | 135 exact original IDs including `none`, plus 7 other IDs |
| All Redux registrations after the release gate | 139 | Includes `none` and 3 registry-only Redux entries without packaged spell definitions |
| Base spell definitions in the audited JAR | 136 | The 135 available original spells and `none` |

`Spells.register` removes `silverfish_swarm`, `conjure_armor`, and the Redux-only `ojospocos` in a production runtime or on a dedicated server. `block_surprise`, `player_hand`, and `wizard_hand` survive that filter despite lacking packaged spell JSON. They are distinct Redux registrations and are excluded from original coverage. `darkvision → dark_vision` and `fireskin → fire_skin` ship under their Redux IDs. `conjure_armour → conjure_armor` exists in source and is gated. Preserve the Redux IDs to protect modern saves.

The 64-entry [original spell inventory](migration/ORIGINAL-SPELL-INVENTORY.md) omits the two shipped aliases from its historical backlog and includes them as context. Of the 64, 10 are release-available with no specific open implementation defect recorded in this audit, 1 has partial parity, 2 are gated, and 51 lack a corresponding cast. Similar spells such as Arc, Homing Spark, Thunderbolt, Fireball, and Phase Step are distinct spells. Icons, sounds, a mob effect, or an entity can exist without a spell registration.

## Completed work and evidence

| Feature | Source evidence and verification |
| --- | --- |
| Iceball | `Spells.ICE_BALL`, `EBEntities`, `EBRenderers`, and `MagicProjectileRenderer` preserve the projectile and its rendering. The user confirmed visible projectile and effects. |
| Frost Ray | `FrostRay`, `RaySpell`, `SpellCastS2C`, and `ClientMessageHandler` preserve hit positions and channel-tick playback. The packaged graphical smoke captured the effect during item use. |
| Greater Telekinesis | `GreaterTelekinesis`, `LevitatingBlock`, `FallingBlockEntityAccessor`, and `EBRenderers` preserve real lift, hold, throw, and landing behavior under the original ID. GameTests cover exact block state, sustained holding, throw velocity, landing, bedrock, and the absence of pig spawning. A packaged screenshot shows the held block and beam. |
| Light | `Light`, `MagicLightBlock`, `MagicLightBlockEntity`, and `MagicLightRenderer` preserve placement, temporary light, and Charm of Light behavior. GameTests cover placement, illumination, and expiry. A packaged screenshot shows the orb. |
| Lectern spell index | `MagicLecternBlock`, `IndexedLecternBlock`, `LecternBlockEntity`, `LecternScreen`, and the open/select payloads preserve registered spell lookup, saved selection, and shelf location. The addon GameTest looks up a Twilight Forest Spell Pack spell. The client smoke exercises opening and locating. |
| Scroll dispensing | `ScrollDispenseBehavior`, `ScrollItem`, and `DispenserCastS2C` preserve location casts, consumption, refill, and playback. GameTests cover powered and blocked conditions and ownerless summon expiry. |
| Nine prior restorations | Blink, Diamondflesh, Empowering Presence, Glide, Ironflesh, Lightning Pulse, Lightning Ray, Summon Ice Giant, and Summon Ice Wraith all have registry bindings and packaged definitions. GameTests cover selected mechanics. The user manually confirmed both ice summons; the earlier scripted visual checks and other coverage are in the inventory and rendering repair notes. |
| Arcane Ascension API | `SpellCastEvent`, `ActiveItemCast`, `WandManaRegenEvent`, and the registry utilities preserve casting and metadata hooks. The registry round-trip and channel-completion tests passed. Progression is deferred. |

The four latest base, combined-addon, Curios, and Accessories profiles each passed 52 GameTests. The packaged dedicated server ran 100 ticks, the packaged graphical client ran 360 ticks, and Fabric Java compilation passed. This audit reused the existing evidence and ran no Gradle tasks.

## Unresolved original spells

Difficulty is an estimate. Low means an existing implementation or reusable foundation with limited new mechanics. Medium means a distinct implementation with focused testing. High means significant state, networking, rendering, AI, world changes, or multiplayer risk.

| Original registry ID | Element | Tier | Current status | Difficulty |
| --- | --- | --- | --- | --- |
| `ebwizardry:black_hole` | sorcery | master | Absent | High |
| `ebwizardry:celestial_smite` | healing | master | Absent | Medium |
| `ebwizardry:chain_lightning` | lightning | advanced | Absent | Medium |
| `ebwizardry:clairvoyance` | sorcery | apprentice | Absent | Medium |
| `ebwizardry:conjure_armour` | sorcery | advanced | Renamed; development-gated | Low |
| `ebwizardry:decoy` | sorcery | advanced | Absent | High |
| `ebwizardry:disintegration` | fire | advanced | Absent | Medium |
| `ebwizardry:divination` | sorcery | apprentice | Absent | Medium |
| `ebwizardry:earthquake` | earth | master | Absent | High |
| `ebwizardry:forcefield` | healing | advanced | Absent | Medium |
| `ebwizardry:forest_of_thorns` | earth | master | Absent | High |
| `ebwizardry:freezing_weapon` | ice | advanced | Absent | Medium |
| `ebwizardry:frost_barrier` | ice | apprentice | Absent | Medium |
| `ebwizardry:grapple` | earth | apprentice | Absent | High |
| `ebwizardry:greater_fireball` | fire | advanced | Absent | Medium |
| `ebwizardry:greater_telekinesis` | sorcery | advanced | Release partial; block entities excluded | High |
| `ebwizardry:ice_shroud` | ice | advanced | Absent | Medium |
| `ebwizardry:ice_statue` | ice | apprentice | Absent | High |
| `ebwizardry:imbue_weapon` | sorcery | apprentice | Absent | Medium |
| `ebwizardry:intimidate` | necromancy | apprentice | Absent | Medium |
| `ebwizardry:lightning_bolt` | lightning | advanced | Absent | Medium |
| `ebwizardry:lightning_disc` | lightning | advanced | Absent | Medium |
| `ebwizardry:lightning_hammer` | lightning | master | Absent | Medium |
| `ebwizardry:lightning_web` | lightning | master | Absent | High |
| `ebwizardry:mark_sacrifice` | healing | apprentice | Absent | High |
| `ebwizardry:mind_control` | necromancy | advanced | Absent | High |
| `ebwizardry:mind_trick` | necromancy | novice | Absent | Medium |
| `ebwizardry:mine` | earth | novice | Absent | Medium |
| `ebwizardry:mirage` | sorcery | advanced | Absent | High |
| `ebwizardry:muffle` | sorcery | novice | Absent | High |
| `ebwizardry:paralysis` | lightning | advanced | Absent spell; effect exists | Low |
| `ebwizardry:petrify` | sorcery | advanced | Absent | High |
| `ebwizardry:possession` | necromancy | master | Absent | High |
| `ebwizardry:radiant_totem` | healing | advanced | Absent | Medium |
| `ebwizardry:resurrection` | healing | master | Absent | High |
| `ebwizardry:shadow_ward` | necromancy | advanced | Absent | Medium |
| `ebwizardry:shield` | healing | apprentice | Absent | Medium |
| `ebwizardry:silverfish_swarm` | earth | master | Minion spell; development-gated | Low |
| `ebwizardry:sixth_sense` | earth | apprentice | Absent | Medium |
| `ebwizardry:slow_time` | sorcery | master | Absent | High |
| `ebwizardry:snare` | earth | novice | Absent | Medium |
| `ebwizardry:snowball` | ice | novice | Absent | Low |
| `ebwizardry:stormcloud` | lightning | advanced | Absent | Medium |
| `ebwizardry:summon_iron_golem` | sorcery | master | Absent | Low |
| `ebwizardry:summon_phoenix` | fire | master | Absent | High |
| `ebwizardry:summon_shadow_wraith` | necromancy | master | Absent spell; entity exists | Low |
| `ebwizardry:summon_spirit_horse` | earth | advanced | Absent | High |
| `ebwizardry:summon_spirit_wolf` | earth | apprentice | Absent | Medium |
| `ebwizardry:summon_storm_elemental` | lightning | master | Absent spell; entity exists | Low |
| `ebwizardry:thunderstorm` | lightning | master | Absent | Medium |
| `ebwizardry:transience` | healing | advanced | Absent | Medium |
| `ebwizardry:transportation` | sorcery | advanced | Absent | High |
| `ebwizardry:wall_of_frost` | ice | master | Absent | High |
| `ebwizardry:withering_totem` | necromancy | advanced | Absent | Medium |

## Known bugs, limits, and verification-only work

- Greater Telekinesis excludes block entities to protect stored data. Test mob and TNT targeting, player protection, artifact interaction, thrown impact damage, and multiplayer before marking full parity. Safe block-entity transport needs a separate design that preserves data and guards against duplication and protection bypass.
- Light has a visibly different ray blend from the original. Charm of Light permanence and dispel are implemented and still need an interactive check. The existing test covers finite lifetime.
- Ice Age already has a release registration and an unrelated source TODO for living ice statues. Its existing behavior does not supply the distinct missing Ice Statue spell.
- Conjure Armour requires a gear-safety review before enabling its existing ID. The current code calls `setItemSlot` after the skip check. Test occupied slots, full inventories, and conjured expiry. Silverfish Swarm needs original count, duration, placement, and targeting checks before removing the gate.
- Captured graphics came from scripted gameplay. The user’s manual confirmation covers Iceball, Summon Ice Giant, and Summon Ice Wraith. Interactive audio, combat, a two-client session, and direct 1.12.2 save conversion remain unverified. Preserve current Redux IDs and test any data migration in copied worlds.

## Compatibility

| Integration | Existing evidence | What remains |
| --- | --- | --- |
| Extra Spells 1.2.0 | Loaded with Twilight Forest in the latest packaged 52-test run. The earlier addon-specific dedicated check cast all 18 spells. | The latest run did not replay that full addon-specific cast matrix or test its visuals. |
| Twilight Forest Spell Pack 1.1.2-port and Twilight Forest 4.8.3345 | Combined packaged tests passed, including real addon lookup in the Redux lectern and registry metadata. Earlier gameplay passed in the addon's own copied smoke world. | The portal assertion failed in the generic Redux fixture. Use the addon fixture for portal checks. Addon-specific dispensing uses its own path. |
| Curios API 9.5.1+1.21.1 | Separate 52-test development GameTest profile passed. | Interactive artifact equip/unequip and telekinesis anchoring are unverified in that profile. |
| Accessories 1.1.0-beta.52+1.21.1 | Separate 52-test development GameTest profile passed. | Interactive equipment behavior and the simultaneous Curios/Accessories setup were not rechecked in the latest evidence. |

No new compatibility failure appeared in the tested profiles. Use compatible 1.21.1 NeoForge addon builds and the same Redux JAR on both sides. Keep addon jars out of baseline smoke runs. See [rendering and casting repair notes](migration/NEOFORGE-REPAIR.md) for prior fixture details.

## Recommended restoration order

1. Start with Paralysis, Summon Shadow Wraith, and Summon Storm Elemental. Confirm their original effects, summon ownership, lifespan, AI, visuals, and sound against the local reference. Reuse the existing effect, entities, and summon infrastructure where suitable.
2. Take a second small batch of Snowball and Summon Iron Golem. Validate Silverfish Swarm and Conjure Armour individually before changing their gates.
3. Group the remaining work by shared mechanics. Tackle projectile and isolated buff spells before persistent constructs, terrain editing, and AI control. Keep player-state spells such as Possession, Resurrection, and Slow Time in separate batches.
4. Track targeted manual verification separately from new implementation. Prioritize Greater Telekinesis targets and protection, Light and Charm of Light, and the recent spells’ audio and multiplayer behavior.

## Source map

- Registry and release gate — [Spells.java](common/src/main/java/com/binaris/wizardry/setup/registries/Spells.java), [NeoForgeDataGenerators.java](neoforge/src/main/java/com/binaris/wizardry/datagen/NeoForgeDataGenerators.java), and [generated spell definitions](common/src/generated/resources/data/ebwizardry/spells).
- Gameplay and rendering coverage — [OriginalParityTest.java](neoforge/src/main/java/com/binaris/wizardry/gametest/OriginalParityTest.java), [NeoForgeMigrationTest.java](neoforge/src/main/java/com/binaris/wizardry/gametest/NeoForgeMigrationTest.java), and [SpellVisualSmoke.java](neoforge/src/main/java/com/binaris/wizardry/client/SpellVisualSmoke.java).
- Resume against the local original checkout at `$env:TEMP/ew-original` when available. Its registry is `src/main/java/electroblob/wizardry/registry/Spells.java` and its definitions are under `src/main/resources/assets/ebwizardry/spells`. The pinned revision is linked in the inventory.

## Resume testing

Run the smallest relevant step first from the Redux root. These PowerShell commands assume a populated dependency cache and JDK 21. Run packaged smoke tasks serially because they share a test directory. Add optional profiles only when the batch touches those integrations or when preparing a release.

```powershell
.\gradlew.bat --offline :neoforge:compileJava
.\gradlew.bat --offline :neoforge:runGameTestServer -PwithoutOptionalIntegrations
.\gradlew.bat --offline :neoforge:runGameTestJarSmoke -PwithoutOptionalIntegrations
.\gradlew.bat --offline :neoforge:runClientJarSmoke -PwithoutOptionalIntegrations
.\gradlew.bat --offline :neoforge:runServerJarSmoke -PwithoutOptionalIntegrations
.\gradlew.bat --offline :neoforge:runGameTestServer -PwithCurios
.\gradlew.bat --offline :neoforge:runGameTestServer -PwithAccessories
.\gradlew.bat --offline :fabric:compileJava
```

For combined addon checks, stage Extra Spells, Twilight Forest Spell Pack, and Twilight Forest in `neoforge/run-packaged-smoke/mods`, run `:neoforge:runGameTestJarSmoke -PwithoutOptionalIntegrations`, then remove only the staged copies. For the Twilight Forest portal check, use a copied addon smoke world. Existing logs are under `build/telekinesis-*.log`; client evidence is under `neoforge/run-packaged-smoke/screenshots`. The current JAR is `neoforge/build/libs/electroblobs-wizardry-redux-1.21.1-0.8.10-neoforge.jar`, SHA-256 `63CF978A1E3B68F5425B411F2418A535E15D3A6C9FE85848F2BBD0D3566C80C9`.

## Git handoff

Branch `fix/neoforge-rendering`, HEAD `4379ba80` (Port Electroblobs Wizardry Redux to NeoForge 1.21.1). At audit start there were 59 modified tracked files and 45 untracked entries, with no staged changes. This audit updates the previously untracked inventory and adds this handoff, leaving 46 untracked entries. Preserve these pending changes and verify with `git status --short --branch`. No commits, pushes, or merges were made.

## Continuation prompt

> Resume Electroblob’s Wizardry Redux 1.21.1 NeoForge on the existing dirty `fix/neoforge-rendering` branch. Read `RESTORATION-PROGRESS.md` and `migration/ORIGINAL-SPELL-INVENTORY.md`, then use the pinned 1.12.2 source to restore Paralysis, Summon Shadow Wraith, and Summon Storm Elemental in a small batch. Preserve existing IDs, rendering, lectern, scroll dispensing, addon and equipment compatibility, and the Arcane Ascension APIs. Leave progression deferred. Start with incremental tests and use packaged client or server checks where the batch warrants them. Record implementation, automated checks, and manual verification separately, and update the counts. Do not commit, push, or merge.
