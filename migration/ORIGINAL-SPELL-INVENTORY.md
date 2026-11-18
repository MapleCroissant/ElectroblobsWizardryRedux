# Original spell comparison

Reference: [Electroblob's Wizardry 4.3.19, Minecraft 1.12.2](https://github.com/Electroblob77/Wizardry/tree/c0be7475b234223e5a3276e62e38e69ffa6de0ce). The comparison starts with all 64 unmatched original IDs from the rendering audit and checks each against the Redux registration source, including entries absent from generated data. The previous audit counted darkvision and fireskin separately, so both appear here for context. Original tier, element and stats are from the 1.12.2 spell definitions.

| Original ID | Element | Tier | Original cost / charge / cooldown | Redux result |
| --- | --- | --- | --- | --- |
| `ebwizardry:black_hole` | sorcery | master | 150 / 25 / 400 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:blink` | sorcery | apprentice | 15 / 0 / 25 | Restored under the original ID. Phase Step remains separate. |
| `ebwizardry:celestial_smite` | healing | master | 100 / 25 / 200 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:chain_lightning` | lightning | advanced | 25 / 5 / 50 | Missing. Arc and Homing Spark are separate original spells. |
| `ebwizardry:clairvoyance` | sorcery | apprentice | 20 / 10 / 100 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:conjure_armour` | sorcery | advanced | 45 / 10 / 50 | Renamed to conjure_armor and development-gated by SENSIBLE. Preserve that Redux ID when enabling it. |
| `ebwizardry:darkvision` | earth | apprentice | 20 / 0 / 40 | Renamed to dark_vision. Included for reference, outside the 64. |
| `ebwizardry:decoy` | sorcery | advanced | 40 / 10 / 200 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:diamondflesh` | healing | master | 100 / 25 / 300 | Restored under the original ID in this pass. |
| `ebwizardry:disintegration` | fire | advanced | 35 / 0 / 40 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:divination` | sorcery | apprentice | 35 / 0 / 80 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:earthquake` | earth | master | 75 / 25 / 250 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:empowering_presence` | healing | apprentice | 30 / 20 / 60 | Restored under the original ID in this pass. |
| `ebwizardry:fireskin` | fire | advanced | 40 / 0 / 250 | Renamed to fire_skin. Included for reference, outside the 64. |
| `ebwizardry:forcefield` | healing | advanced | 45 / 15 / 200 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:forest_of_thorns` | earth | master | 100 / 20 / 250 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:freezing_weapon` | ice | advanced | 35 / 0 / 70 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:frost_barrier` | ice | apprentice | 20 / 0 / 50 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:glide` | earth | advanced | 5 / 0 / 0 | Restored under the original ID in this pass. |
| `ebwizardry:grapple` | earth | apprentice | 5 / 0 / 0 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:greater_fireball` | fire | advanced | 20 / 5 / 30 | Missing. Fireball is a separate original spell. |
| `ebwizardry:greater_telekinesis` | sorcery | advanced | 10 / 0 / 0 | Restored under the existing ID. Channels to lift blocks and entities and throws while sneaking. Block entities are excluded to protect stored data. The surprise spell remains separate. |
| `ebwizardry:ice_shroud` | ice | advanced | 40 / 0 / 250 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:ice_statue` | ice | apprentice | 15 / 0 / 40 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:imbue_weapon` | sorcery | apprentice | 20 / 0 / 50 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:intimidate` | necromancy | apprentice | 20 / 0 / 100 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:ironflesh` | healing | advanced | 30 / 15 / 100 | Restored under the original ID in this pass. |
| `ebwizardry:light` | sorcery | novice | 5 / 0 / 15 | Restored as a temporary light orb with Charm of Light permanence and removal. |
| `ebwizardry:lightning_bolt` | lightning | advanced | 40 / 10 / 80 | Missing. Thunderbolt and Arc are separate original spells. |
| `ebwizardry:lightning_disc` | lightning | advanced | 25 / 5 / 60 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:lightning_hammer` | lightning | master | 100 / 25 / 300 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:lightning_pulse` | lightning | advanced | 25 / 0 / 75 | Previously development-only. Enabled for release after restoring the two original sound events and fixing knockback. |
| `ebwizardry:lightning_ray` | lightning | apprentice | 5 / 0 / 0 | Restored under the original ID in this pass. |
| `ebwizardry:lightning_web` | lightning | master | 15 / 15 / 70 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:mark_sacrifice` | healing | apprentice | 25 / 0 / 60 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:mind_control` | necromancy | advanced | 40 / 10 / 150 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:mind_trick` | necromancy | novice | 10 / 0 / 40 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:mine` | earth | novice | 5 / 0 / 5 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:mirage` | sorcery | advanced | 40 / 10 / 200 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:muffle` | sorcery | novice | 5 / 0 / 25 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:paralysis` | lightning | advanced | 20 / 10 / 60 | Missing spell. The paralysis status effect already exists. |
| `ebwizardry:petrify` | sorcery | advanced | 40 / 20 / 100 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:possession` | necromancy | master | 100 / 15 / 300 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:radiant_totem` | healing | advanced | 40 / 15 / 175 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:resurrection` | healing | master | 150 / 25 / 400 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:shadow_ward` | necromancy | advanced | 10 / 0 / 0 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:shield` | healing | apprentice | 5 / 0 / 0 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:silverfish_swarm` | earth | master | 80 / 20 / 300 | Development-only MinionSpell implementation, gated by SENSIBLE. |
| `ebwizardry:sixth_sense` | earth | apprentice | 20 / 0 / 100 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:slow_time` | sorcery | master | 100 / 15 / 200 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:snare` | earth | novice | 10 / 0 / 10 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:snowball` | ice | novice | 1 / 0 / 1 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:stormcloud` | lightning | advanced | 60 / 10 / 150 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:summon_ice_giant` | ice | master | 100 / 20 / 400 | Previously development-only. Enabled for release using the existing giant and its original texture. |
| `ebwizardry:summon_ice_wraith` | ice | advanced | 40 / 10 / 200 | Restored under the original ID with the existing ice wraith entity. |
| `ebwizardry:summon_iron_golem` | sorcery | master | 175 / 20 / 400 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:summon_phoenix` | fire | master | 150 / 20 / 400 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:summon_shadow_wraith` | necromancy | master | 100 / 20 / 400 | Missing spell. A shadow wraith entity already exists. |
| `ebwizardry:summon_spirit_horse` | earth | advanced | 50 / 10 / 150 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:summon_spirit_wolf` | earth | apprentice | 25 / 0 / 100 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:summon_storm_elemental` | lightning | master | 100 / 20 / 400 | Missing spell. A storm elemental entity already exists. |
| `ebwizardry:thunderstorm` | lightning | master | 100 / 20 / 250 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:transience` | healing | advanced | 50 / 15 / 100 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:transportation` | sorcery | advanced | 100 / 20 / 100 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:wall_of_frost` | ice | master | 15 / 15 / 70 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:withering_totem` | necromancy | advanced | 50 / 20 / 200 | Missing. No corresponding Redux spell registration. |

## Restoration details

- **Blink** keeps its original block-targeted teleport and NPC repositioning.
  It uses the mounted teleport artifact if equipped, plays the original audio,
  and restores the first-person overlay and FOV pulse behind a client toggle.
- **Ironflesh and Diamondflesh** use the original duration and cast settings.
  Both add armor; Ironflesh adds knockback resistance and Diamondflesh adds
  armor toughness. Both restore the original mineral overlay and configurable
  movement penalty.
- **Lightning Ray** brings back the aimed, continuous lightning arc, ten-tick
  damage cadence, spark particles and looping audio. **Lightning Pulse** now
  ships in release builds with the original two-part audio and ground effect.
- **Summon Ice Wraith and Summon Ice Giant** use the original summon defaults
  and existing entity types. The Ice Giant's packaged texture matches the
  original byte-for-byte. Its spell previously registered only in development.
- **Glide** uses the original downward velocity and potency-scaled horizontal
  acceleration, leaf and sparkle particles, and wind loop.
- **Empowering Presence** buffs allied players and spell-casting mobs, uses
  the original radius and duration, and stacks potency multiplicatively without
  amplifying another cast of itself. The original seasonal display-name
  variation remains open.

## Base-mod interactions

- Any of the six base lecterns opens the spell index. The shelf scan reads
  registered spell IDs from nearby spell books, including addon book subclasses,
  and groups duplicate copies by shelf. It defaults to the original four-block
  search radius, with server options for radius and extra inventory blocks.
  The index supports sorting, paging, keyword search and the original
  field-query syntax, for example tier=advanced;element=ice. Selection is
  validated against loaded shelves on the server and saved by namespaced ID.
  Locate Book uses the original highlight sprite and sound.
- The base spell scroll now casts from dispensers. Blocked outlets, cancellation
  and failed casts retain the scroll. Unsupported spells are ejected normally.
  A continuous cast uses matching scrolls to refill while the dispenser remains
  powered, and ends when interrupted. The active cast is transient, matching
  the original across unloads. Location casts emit dispenser spell events;
  player completion events are unchanged.
- Exercising the dispenser exposed an existing crash when tracing without a
  caster. The fix applies before Minecraft builds the collision context.
  Source-less Arc, Guardian Beam, Ray of Purification and Trap Slime now
  handle the absent caster in their applicable paths. Positional area spells
  can apply their effects without a player ally filter, as in the original.
  Dispenser minions expire on schedule and keep their ownerless target goals.

## Remaining work

51 spells lack a corresponding casting implementation. Silverfish Swarm and
the renamed Conjure Armour are development-gated. Greater Telekinesis is
release-playable with a block-entity parity gap. Together, 54 of the 64
tracked entries have open implementation or release work. See
[Restoration progress](../RESTORATION-PROGRESS.md) for the full registry count,
per-spell estimates, and verification-only work. Direct 1.12.2 save conversion and uninterrupted channel resumption after chunk unload
are outside the verified Redux save behavior. The lectern currently uses modern
widgets on the original book textures. Full visual, combat, and multiplayer
verification across all restored spells is still open.

## Test evidence

- Packaged GameTests cover lectern lookup and saved selection, blocked and
  powered dispenser behavior, scroll refill, location-cast summon expiry,
  original mineral buffs, Lightning Ray damage cadence, Glide motion, and
  multiplicative Empowerment. The combined addon profile uses an actual
  registered Twilight Forest Spell Pack ID for the lectern test and checks
  metadata round-trips across the runtime spell registry.
- The packaged NeoForge client exercises Iceball and Frost Ray, enters the
  spell index through block interaction, opens the Ironflesh page, and locates
  the correct shelf. Captured frames are in the smoke instance screenshots
  folder as parity-lectern.png, parity-lectern-spell.png, and
  parity-lectern-locate.png. Those frames were inspected separately from the
  automated assertions.
- The dedicated server smoke world starts and ticks. The optional equipment
  profiles are separate GameTest runs. The build logs are parity-release.log,
  parity-release-addons.log, parity-release-curios.log, and
  parity-release-accessories.log under build.
- Package inspection checked all nine restored spell definitions and icons.
  Their spell IDs, tiers, elements, mana costs, charge times, and cooldowns
  match the original definitions. Interactive combat, all audio cues, and a
  two-client multiplayer session remain unverified.
- Greater Telekinesis and Light retain their original spell metadata and use
  existing Wizardry sound and visual assets. The packaged GameTests cover
  telekinetic lifting, sustained holding, block state preservation, throwing,
  landing, and bedrock protection, plus Light placement, brightness, and expiry.
- The packaged client captured and inspected the held block and beam in
  parity-greater-telekinesis.png and the orb in parity-magic-light.png.
  The user's separate interactive checks confirmed Iceball, Summon Ice Giant,
  and Summon Ice Wraith. Interactive Greater Telekinesis and Light combat,
  audio, and multiplayer checks remain open.
- The follow-up ran 52 passing GameTests in each of the base, combined addon,
  Curios, and Accessories profiles. The packaged dedicated server completed
  100 ticks, and the Fabric Java sources compiled. See the telekinesis logs
  under build for the individual runs.