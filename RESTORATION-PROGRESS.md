# Electroblob’s Wizardry Redux 1.21.1 NeoForge restoration progress

## Registry audit

The 1.12.2 reference at commit `c0be7475b234223e5a3276e62e38e69ffa6de0ce` has 189 spell definitions and 189 registration calls, including the `none` sentinel. Counts below compare the original definitions with the central Redux declarations and release filter, then cross-check the local packaged JAR. The matching counts measure casting implementation and release availability. They do not establish full mechanics parity.

| Scope | Entries | Basis |
| --- | ---: | --- |
| Original playable spells | 188 | Excludes `none` |
| Original spells with Redux casting implementations in source | 188 | 185 original IDs and 3 renamed equivalents. Includes partial Greater Telekinesis |
| Original spells available in the default Redux release registry | 188 | 185 original IDs and 3 release aliases. Includes partial Greater Telekinesis |
| Original spells unavailable in the default Redux release | 0 | All originals have a release definition |
| All Redux source declarations | 193 | 186 exact original IDs including `none`, plus 7 other IDs |
| All Redux registrations after the release gate | 192 | Includes `none` and 3 registry-only Redux entries without packaged spell definitions |
| Base spell definitions in the audited JAR | 189 | The 188 available original spells and `none` |

`Spells.register` removes the Redux-only `ojospocos` in a production runtime or on a dedicated server. `block_surprise`, `player_hand`, and `wizard_hand` survive that filter despite lacking packaged spell JSON. They are distinct Redux registrations and are excluded from original coverage. `darkvision → dark_vision` and `fireskin → fire_skin` ship under their Redux IDs. `conjure_armour → conjure_armor` ships under the existing Redux ID. Preserve the Redux IDs to protect modern saves.

The 64-entry [original spell inventory](migration/ORIGINAL-SPELL-INVENTORY.md) omits the two shipped aliases from its historical backlog and includes them as context. All 64 are release-available. Availability counts include known mechanical differences such as Greater Telekinesis block entities and Possession host behavior. Similar spells such as Arc, Homing Spark, Thunderbolt, Fireball, and Phase Step are distinct spells. Icons, sounds, a mob effect, or an entity can exist without a spell registration.

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
| Paralysis | The original shock ray reuses the Paralysis effect. It suspends mob AI, limits player movement and camera input, and releases the victim when damage brings health to the critical threshold. |
| Summon Shadow Wraith | Summons the existing particle-based wraith with the original 600-tick lifetime, stats, Darkness Orb attack, and sounds. |
| Summon Storm Elemental | Summons the existing particle-based elemental for 600 ticks. Its ranged attack launches an owned, seeking Lightning Disc with shock damage and piercing. The disc is an entity dependency. The separate player spell remains in the inventory. |
| Arcane Ascension API | `SpellCastEvent`, `ActiveItemCast`, `WandManaRegenEvent`, and the registry utilities preserve casting and metadata hooks. The registry round-trip and channel-completion tests passed. Progression is deferred. |

The previous batch passed 52 GameTests in each of the base, combined addon, Curios, and Accessories profiles, plus a 100-tick dedicated-server smoke and Fabric Java compilation. The three-spell batch passed incremental NeoForge compilation and 55 packaged GameTests. The packaged client booted with the new mixin and completed its 360-tick smoke. That smoke did not cast this batch interactively. The packaged JAR includes the three icons, and each spell definition matches the pinned original tier, element, type, cost, charge time, cooldown, and base properties.

## Latest restoration work

- Snowball launches a vanilla snowball with ballistic range scaling. The original does not support dispenser casting. Lightning Disc uses the seeking, piercing projectile already restored for Storm Elemental.
- Summon Spectral Golem creates the original golem type and a 600-tick minion using the original texture. Silverfish Swarm now uses dedicated non-infesting minions that multiply on kills, up to generation five.
- Mind Trick clears and suppresses mob targets until attacked and applies nausea to player targets. Celestial Smite strikes terrain below a golden beam and applies area damage and fire.
- Conjure Armour preserves equipped items, skips existing spectral pieces, equips only eligible slots, and uses server-authoritative inventory changes. Greater Fireball restores the explosive projectile, reflection, and blast scaling.
- Mark Sacrifice amplifies the next magic hit by 60 percent at base strength and consumes the mark. On NeoForge, the incoming damage adjustment now reaches its damage container. Ice Shroud uses the original 600-tick buff and applies 100 ticks of frost to a nearby nonprojectile attacker.
- Creative spell books and scrolls enumerate the runtime registry. Original IDs with no runtime registration are absent from those tabs. Existing Redux-only registry entries still resolve saved IDs and are outside the original count. Random spell loot applies the loot context and book or scroll availability filters. Restoration does not require alternate saved IDs or addon source changes. Tests cover book and scroll binding and wand resolution for Snowball and Lightning Disc. Creative search, tooltips, live item use, and multiplayer remain to verify.

The final effect batch passed 64 required GameTests in both the development and packaged NeoForge runs, with Fabric Java compilation. Package inspection matched all ten restored spell definitions against 1.12.2 and found their icons. The combined Extra Spells and Twilight Forest profile and the separate Curios and Accessories profiles each passed 64 GameTests. The packaged client and dedicated server completed their boot smokes. Actual screenshots of the new spells, interactive audio, reconnect behavior, and multiplayer play remain unverified.

### Frost Barrier, Imbue Weapon, and Mine checkpoint

Frost Barrier has its arc, original model, tier-scaled panel count, and timed collision. Imbue Weapon uses temporary sword and bow enchantments with separate active limits and original damage bonuses. Mine checks each block against its harvest tier and protection, and supports Charm of Silk Touch. All 67 development GameTests passed with these three spells.

Greater Telekinesis can now carry closed, unlocked shulker boxes. A GameTest covers the Arcane Lock check, saved carrier data, exact landing inventory, and one recovery item on expiry. The development suite reached 68 passing tests. Other block entities remain excluded.

Divination restores the loaded-chunk ore scan, relative feedback, flash hints, and strong-signal nudge. It uses an ore tag and deterministic value weights instead of sampling 1.12.2 mining and smelting XP. Freezing Weapon builds on the same temporary enchantment infrastructure, with 200 ticks of frost per level for melee hits and 150 for arrows. Both passed the 70-test development and packaged profiles. The non-shulker block-entity gap remains open. Chests and similar containers require a transfer path that accounts for on-removal item drops and multi-block partners.

Chain Lightning restores the primary and two subsequent shock tiers, capped target lists, ally filtering, and arc effects. Muffle restores temporary sound suppression and the 144-degree targeting arc. Development and packaged GameTests passed with both. The Frost Barrier smoke captured the original three-panel model in the packaged client at parity-frost-barrier.png.

Intimidate applies the original timed fear in an eight-block area, clears targeting, and attempts to route affected creatures away from their loaded fear source. The fear-source UUID and effect persist across saves. Its tests cover duration, targeting suppression and save/load. Navigation under real multiplayer conditions remains unverified. The final checkpoint passed 73 GameTests in development, the packaged JAR, the combined addon profile, Curios, and Accessories. Packaged client and dedicated-server smoke checks and Fabric Java compilation passed. The JAR includes all eight definitions and icons, with their original spell metadata and base property values verified.

### Lightning, traps, and spirit companions checkpoint

Lightning Bolt and Thunderstorm use the vanilla lightning entity with stored caster and damage information. They require outdoor placement, and spell fire is checked per block. Spell strikes retain permitted lightning-rod activation. Copper cleaning is left to natural lightning because its vanilla routine can modify neighboring blocks. Stormcloud retains five-block placement, target-age strike cadence, particles, original sounds, and Ring of Stormcloud movement. Snare restores a saved-owner single-use trap. Spirit Wolf restores the persistent second-cast replacement rule, tamed ownership, potency scaling, and owner dismissal. Spirit Horse restores the owned mount, intrinsic saddle, potency scaling, and idle fade. All 79 GameTests passed in development and from the packaged JAR. Packaged dedicated-server and integrated-client smoke checks passed, and Fabric compiled. The combined Extra Spells and Twilight Forest packaged profile and the separate Curios and Accessories development profiles each passed 79 GameTests. The six spells still need interactive visual and audio checks.

Lightning Hammer restores the outdoor fall, hard impact, timed ground shocks and owner-bound ring pickup and throw. Lightning Web restores a continuous ray with primary, secondary, and tertiary damage at ten-tick intervals. All 82 GameTests passed in development and from the packaged JAR. The dedicated-server and integrated-client smoke checks passed, and Fabric compiled. Interactive audiovisual testing of both spells remains open.

Shield restores continuous Resistance I, projectile deflection, the animated shield and release cleanup. Shadow Ward restores a separate channeled defense that splits eligible damage with the attacker and prevents recursive reflection. The development suite passed 84 GameTests. Both spells are included in the subsequent packaged validation.

Disintegration restores the fire ray, lethal-only ember burst, corpse overlay and Ring of Disintegration. The development suite passed 85 GameTests, including nonlethal ignition, exactly-once burst and ember ignition and expiry.

Radiant Totem prioritizes healing wounded allies. Withering Totem stores successful drains and discharges them with Wither on expiry. Clairvoyance remembers a clicked face and dimension, routes through loaded terrain under a 5,000-node budget, and sends the trail only to the caster. Forest of Thorns grows a timed two-block-high ring. Black Hole restores hostile attraction, close-range damage, and charm-gated terrain interaction. All 90 required GameTests passed in development and from the packaged JAR. The packaged dedicated-server and integrated-client smoke checks passed, and Fabric compiled.

Transience restores invisibility, incoming and outgoing damage restrictions, and interaction blocking. Its 1.12.2 rules leave normal block collision in place. Sixth Sense restores the range-scaled effect, through-wall creature markers, and Dire Wolf Pelt marker shapes. All 92 GameTests passed in development, from the packaged JAR, with both spell-pack addons together, and in separate Curios and Accessories profiles. The packaged client loaded both post chains and cleared them on expiry, with screenshots. Dedicated-server smoke and Fabric compilation passed.

Grapple restores the extending vine, block-anchor movement, entity reeling, abseiling artifact, and release. The development suite passed 93 GameTests, including anchor removal and a location-cast pull.

Mind Control restores timed controller-aware targeting, saved controller identity, target cleanup, sheep recoloring, the aura layer, and the Ring of Mind Control. All 94 GameTests passed in development and from the packaged JAR. The packaged NeoForge client and dedicated server and Fabric compilation passed.

Decoy restores opposite lateral movement, owner appearance, invulnerability, time limit, and mob aggro diversion. Mirage restores hidden sustained particles, shifted translucent rendering, and mob target suppression. Summon Phoenix restores the fire-immune hovering minion and its sustained Flame Ray attack. All 97 GameTests passed in development and from the packaged JAR. The packaged client received an owner-resolved Decoy and applied Mirage to it after the mob effect synchronization fix. Dedicated-server smoke and Fabric compilation passed.

Earthquake restores the expanding wave, damage and Weakness, vertical launch, audiovisual effects, and protected terrain bounce. Greater Telekinesis now transports closed, unlocked barrels, with no source inventory drops and an inventory-bearing recovery item if placement fails. Shulker support remains covered. All 99 GameTests passed in development and from the packaged JAR. The packaged client and dedicated-server smoke checks passed, and Fabric compiled.

Ice Statue, Petrify, and Wall of Frost share transactional mob storage and a saved release queue. Tests cover a blocked conversion, restoration of UUID, health and equipment, releasing by breaking an upper section, and a wall thawing without water. All 102 GameTests passed in development and from the packaged JAR. A packaged client retry passed after the illusion smoke fixture began tracking the specific Decoy it created. Dedicated-server smoke and Fabric compilation passed. Wall of Frost reuses the existing DryFrostedIceBlock used by Permafrost.

Slow Time restores the 300-tick aura, original every-sixth-tick cadence, potency scaling, Stop Time charm, client shaders, and scoped player input and update pausing. Overlapping auras select the strongest schedule. All 103 GameTests passed in development and from the packaged JAR.

Forcefield restores the persistent spherical boundary, projectile deflection, damage and interaction protection, and explosion filtering. Transportation restores craftable stones, remembered circles, the Ancient Compass selection overlay, countdown and arrival checks, and Spectral Tether mount transport. All 105 development and packaged GameTests passed including Forcefield boundary damage and Transportation delay and broken-circle rejection.

## Final spell batch

- Resurrection selects a nearby, same-dimension dead ally after the original 200-tick wait, scaled by potency. The native player-list respawn replaces the dead player at a clear location near the death site, restores half health, clears experience, and plays revival effects. The Amulet of the Immortal authorizes a self-cast with a hotbar wand or scroll. The server retains a single casting stack when inventory drops and applies normal item casting costs. The scroll cooldown is reapplied after the server player is replaced. The death record and retained inventory are stored in player NBT. Addons can cancel the operation through the Resurrection event.
- Possession acquires a mob through the original ray and transfers movement, view, melee, and special attacks. The real host remains tracked with AI paused. The player retains their normal inventory behind input restrictions. Cleanup removes host ownership on sneak, expiry, invalidation, death, game mode change, dimension change, or logout. Tests cover scroll casting, exclusive control, movement, manual exit, removal, and Enderman block carry and placement.
- The pinned release audit accounted for all 188 IDs. Both new spells match the original tier, element, type, mana, charge, cooldown, and main properties. The inventory lists 23 existing definitions with metadata differences.

## Known bugs, limits, and verification-only work

- Possession keeps a real mob in the world and uses the normal player controller. The camera uses the host eye height, while the player collision box remains vanilla. The first-person view hides the player hand model. The Wither, Ender Dragon, and mounted hosts are excluded. Enderman extraction uses a server ray, checks break permissions, and excludes block entities. Exact mob target priorities and individual species combat still need comparison.
- Resurrection needs interactive checks of the death-screen action, both inventory rules, Curios and Accessories amulet slots, reconnects, and dimension travel. GameTests exercise the allied and vanilla amulet paths through the real server player list. An actual second client was not used.

- Greater Telekinesis preserves closed, unlocked shulker boxes and barrels. Other block entities, including multi-block containers, remain excluded. Test mob and TNT targeting, player protection, artifact interaction, thrown impact damage, and multiplayer before marking full parity. Other block types need dedicated removal and placement handling to preserve data and guard against duplication.
- Light has a visibly different ray blend from the original. Charm of Light permanence and dispel are implemented and still need an interactive check. The existing test covers finite lifetime.
- Ice Age already has a release registration and an unrelated source TODO for living ice statues. Its existing behavior does not supply the distinct missing Ice Statue spell.
- Conjure Armour now passes occupied-slot, full-inventory, duplicate prevention, duration, expiry, and death-cleanup checks. Reconnect and live equipment synchronization still need interactive verification. Silverfish placement and ownership under multiplayer load remain unverified.
- Captured graphics came from scripted gameplay. The user’s manual confirmation covers Iceball, Summon Ice Giant, and Summon Ice Wraith. Interactive audio, combat, a two-client session, and direct 1.12.2 save conversion remain unverified. Preserve current Redux IDs and test any data migration in copied worlds.

- The three-spell batch has automated server coverage for Paralysis damage, effect duration and critical release, summon type and lifetime, and the elemental’s disc launch, impact damage, and piercing. Player look lock, in-game particles and audio, and summon behavior in multiplayer remain unverified interactively. Storm Elemental uses the modern ranged goal with the original 15-block range and 90-tick repeat interval. Its first attack and melee transitions follow that goal’s scheduling.

## Compatibility

| Integration | Existing evidence | What remains |
| --- | --- | --- |
| Extra Spells 1.2.0 | Loaded with Twilight Forest in the packaged 111-test run. The earlier addon-specific dedicated check cast all 18 spells. | The latest run did not replay that full addon-specific cast matrix or test its visuals. |
| Twilight Forest Spell Pack 1.1.2-port and Twilight Forest 4.8.3345 | Combined 111-test packaged run passed, including real addon lookup in the Redux lectern and registry metadata. Earlier gameplay passed in the addon's own copied smoke world. | The portal assertion failed in the generic Redux fixture. Use the addon fixture for portal checks. Addon-specific dispensing uses its own path. |
| Curios API 9.5.1+1.21.1 | Separate 111-test development GameTest profile passed. | Interactive artifact equip/unequip and telekinesis anchoring are unverified in that profile. |
| Accessories 1.1.0-beta.52+1.21.1 | Separate 111-test development GameTest profile passed. | Interactive equipment behavior and the simultaneous Curios/Accessories setup were not rechecked in the latest evidence. |

No new compatibility failure appeared in the tested profiles. Use compatible 1.21.1 NeoForge addon builds and the same Redux JAR on both sides. Keep addon jars out of baseline smoke runs. See [rendering and casting repair notes](migration/NEOFORGE-REPAIR.md) for prior fixture details.

## Remaining verification

1. Play the amulet death-screen flow with a wand and scroll, both inventory rules, a live second client, and optional artifact equipment.
2. Check Possession audio, host visuals, varied host movement, attacks, death and dimension travel, logout, and input recovery with two clients.
3. Revisit other Greater Telekinesis block entities independently.

The final GameTests use NeoForge-negotiated embedded connections for real player-list respawn, retained wand and scroll accounting, and logout cleanup. The test for Phoenix hover keeps its mobs persistent and runs at a separate height so the embedded players cannot change its natural despawn behavior.

## Source map

- Final gameplay: [Resurrection.java](common/src/main/java/com/binaris/wizardry/content/spell/healing/Resurrection.java) and [Possession.java](common/src/main/java/com/binaris/wizardry/content/spell/necromancy/Possession.java).
- Previous batch: [Forcefield.java](common/src/main/java/com/binaris/wizardry/content/spell/healing/Forcefield.java), [Transportation.java](common/src/main/java/com/binaris/wizardry/content/spell/sorcery/Transportation.java), [TransportationStoneBlock.java](common/src/main/java/com/binaris/wizardry/content/block/TransportationStoneBlock.java), and [TransportationOverlay.java](common/src/main/java/com/binaris/wizardry/client/effect/TransportationOverlay.java).
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

For combined addon checks, stage Extra Spells, Twilight Forest Spell Pack, and Twilight Forest in `neoforge/run-packaged-smoke/mods`, run `:neoforge:runGameTestJarSmoke -PwithoutOptionalIntegrations`, then remove only the staged copies. For the Twilight Forest portal check, use a copied addon smoke world. Previous logs are under `build/telekinesis-*.log`. See `build/original-spell-batch-*.log` for the prior batch and `build/restoration-fear-*.log` for the prior checkpoint and `build/restoration-spirit-release.log` for the previous 79-test checkpoint, with optional profile logs in `build/restoration-spirit-addons.log`, `build/restoration-spirit-curios.log`, and `build/restoration-spirit-accessories.log`. See `build/restoration-hammer-release.log` for the 82-test checkpoint and `build/restoration-constructs-release.log` for the 90-test checkpoint. The 92-test checkpoint is under `build/restoration-vision-*.log`. The 94-test checkpoint is under `build/restoration-control-*.log`. The 97-test checkpoint is under `build/restoration-phoenix-*.log`. The combined addon, Curios, and Accessories profiles each passed 99 required GameTests. The 99-test checkpoint is under `build/restoration-quake-*.log`. The base 186-spell packaged validation is in `build/restoration-186-release.log`. The 188-spell development and packaged runs each passed 111 required GameTests in `build/restoration-188-final-release.log`. The dedicated server reached 100 ticks and the client reached 680 ticks, including the scripted Possession shader and camera restoration. Fabric Java compilation passed in the same command. The combined addon, Curios, and Accessories profiles each passed their 111-test run in build/restoration-188-final-addons.log, build/restoration-188-final-curios.log, and build/restoration-188-final-accessories.log. The two amulet-specific fixtures use the vanilla artifact integration, so optional equipment slots still need an interactive check. The Forcefield and Transportation validation logs are `build/restoration-transportation-*.log`. The combined addon suite also verifies Transportation?s exact tier, element, mana cost, charge time, and cooldown from the packaged registry. See `build/restoration-statues-verified.log` for 102 packaged tests and `build/restoration-slow-time-verified.log` for 103. Client evidence is under `neoforge/run-packaged-smoke/screenshots`. The current JAR is `neoforge/build/libs/electroblobs-wizardry-redux-1.21.1-0.8.10-neoforge.jar`, SHA-256 `AF42F64514EE84DC29052006B6C7479C895625CBA59D21BB0A7ACB5F71F71795`.

## Git handoff

Branch `fix/neoforge-rendering`, HEAD `b6c67033`. The working tree has 20 modified tracked files and 13 untracked files, with no staged changes. No commits, pushes, or merges were made during this restoration session.

## Continuation prompt

> Resume Electroblob’s Wizardry Redux 1.21.1 NeoForge on the existing fix/neoforge-rendering branch. Read both restoration documents and continue with the remaining manual checks, mechanics gaps, and metadata differences. Preserve registry aliases, addon and equipment compatibility, and the Arcane Ascension APIs. Leave progression deferred.
