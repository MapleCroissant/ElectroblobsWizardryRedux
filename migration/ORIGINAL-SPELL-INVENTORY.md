# Original spell comparison

Reference: [Electroblob's Wizardry 4.3.19, Minecraft 1.12.2](https://github.com/Electroblob77/Wizardry/tree/c0be7475b234223e5a3276e62e38e69ffa6de0ce). The comparison starts with all 64 unmatched original IDs from the rendering audit and checks each against the Redux registration source, including entries absent from generated data. The previous audit counted darkvision and fireskin separately, so both appear here for context. Original tier, element and stats are from the 1.12.2 spell definitions.

| Original ID | Element | Tier | Original cost / charge / cooldown | Redux result |
| --- | --- | --- | --- | --- |
| `ebwizardry:black_hole` | sorcery | master | 150 / 25 / 400 | Restored attraction, core damage, original animated rays, and protected charm terrain interaction. |
| `ebwizardry:blink` | sorcery | apprentice | 15 / 0 / 25 | Restored under the original ID. Phase Step remains separate. |
| `ebwizardry:celestial_smite` | healing | master | 100 / 25 / 200 | Restored with terrain targeting, golden celestial strike, and area damage and fire. |
| `ebwizardry:chain_lightning` | lightning | advanced | 25 / 5 / 50 | Restored with primary, secondary, and tertiary arcs, distinct damage, and target caps. |
| `ebwizardry:clairvoyance` | sorcery | apprentice | 20 / 10 / 100 | Restored saved destination, loaded-chunk pathfinding and caster-only trail. |
| `ebwizardry:conjure_armour` | sorcery | advanced | 45 / 10 / 50 | Enabled under the existing Redux ID conjure_armor with occupied-slot and duplicate checks. |
| `ebwizardry:darkvision` | earth | apprentice | 20 / 0 / 40 | Renamed to dark_vision. Included for reference, outside the 64. |
| `ebwizardry:decoy` | sorcery | advanced | 40 / 10 / 200 | Restored owned visual double, lateral split, aggro diversion, and expiry. |
| `ebwizardry:diamondflesh` | healing | master | 100 / 25 / 300 | Restored under the original ID in this pass. |
| `ebwizardry:disintegration` | fire | advanced | 35 / 0 / 40 | Restored fire ray, lethal ember burst, animated overlay and artifact. |
| `ebwizardry:divination` | sorcery | apprentice | 35 / 0 / 80 | Restored with weighted ore search, directional hints, and a strong-signal nudge. |
| `ebwizardry:earthquake` | earth | master | 75 / 25 / 250 | Restored expanding ground wave, damage, Weakness, launch and protected debris. |
| `ebwizardry:empowering_presence` | healing | apprentice | 30 / 20 / 60 | Restored under the original ID in this pass. |
| `ebwizardry:fireskin` | fire | advanced | 40 / 0 / 250 | Renamed to fire_skin. Included for reference, outside the 64. |
| `ebwizardry:forcefield` | healing | advanced | 45 / 15 / 200 | Restored persistent sphere, projectile deflection, and cross-boundary damage, interaction, and explosion rules. |
| `ebwizardry:forest_of_thorns` | earth | master | 100 / 20 / 250 | Restored expanding two-block thorn ring with owner-aware damage and expiry. |
| `ebwizardry:freezing_weapon` | ice | advanced | 35 / 0 / 70 | Restored with timed sword and bow imbuement and separate melee and arrow frost durations. |
| `ebwizardry:frost_barrier` | ice | apprentice | 20 / 0 / 50 | Restored with the original timed ice panel arc, collision, sounds, model, and texture. |
| `ebwizardry:glide` | earth | advanced | 5 / 0 / 0 | Restored under the original ID in this pass. |
| `ebwizardry:grapple` | earth | apprentice | 5 / 0 / 0 | Restored extending vine, two-way reeling, abseiling, and release. |
| `ebwizardry:greater_fireball` | fire | advanced | 20 / 5 / 30 | Restored with a larger fireball, explosion, blast modifier, and reflection. |
| `ebwizardry:greater_telekinesis` | sorcery | advanced | 10 / 0 / 0 | Restored under the existing ID. Channels to lift blocks and entities and throws while sneaking. Closed, unlocked shulker boxes preserve their data. Other block entities remain excluded. The surprise spell remains separate. |
| `ebwizardry:ice_shroud` | ice | advanced | 40 / 0 / 250 | Restored with a 600-tick shroud and reactive frost on nearby nonprojectile attackers. |
| `ebwizardry:ice_statue` | ice | apprentice | 15 / 0 / 40 | Restored temporary mob conversion and saved thaw recovery. |
| `ebwizardry:imbue_weapon` | sorcery | apprentice | 20 / 0 / 50 | Restored with separate timed magic sword and bow enchantments and bonus damage. |
| `ebwizardry:intimidate` | necromancy | apprentice | 20 / 0 / 100 | Restored original area fear, targeting suppression, fleeing navigation, and persisted source. |
| `ebwizardry:ironflesh` | healing | advanced | 30 / 15 / 100 | Restored under the original ID in this pass. |
| `ebwizardry:light` | sorcery | novice | 5 / 0 / 15 | Restored as a temporary light orb with Charm of Light permanence and removal. |
| `ebwizardry:lightning_bolt` | lightning | advanced | 40 / 10 / 80 | Restored outdoor terrain targeting and vanilla bolts with configurable strike damage, caster attribution, and protected fire placement. |
| `ebwizardry:lightning_disc` | lightning | advanced | 25 / 5 / 60 | Restored as a player-cast spell using the existing seeking, piercing projectile. |
| `ebwizardry:lightning_hammer` | lightning | master | 100 / 25 / 300 | Restored falling construct, impact strike, timed ground attack and owner-bound ring transfer. |
| `ebwizardry:lightning_pulse` | lightning | advanced | 25 / 0 / 75 | Previously development-only. Enabled for release after restoring the two original sound events and fixing knockback. |
| `ebwizardry:lightning_ray` | lightning | apprentice | 5 / 0 / 0 | Restored under the original ID in this pass. |
| `ebwizardry:lightning_web` | lightning | master | 15 / 15 / 70 | Restored continuous ray, three damage tiers, chain caps, and free-end beam. |
| `ebwizardry:mark_sacrifice` | healing | apprentice | 25 / 0 / 60 | Restored with a 200-tick mark consumed by the next magic hit, which gains a damage bonus. |
| `ebwizardry:mind_control` | necromancy | advanced | 40 / 10 / 150 | Restored temporary retargeting, controller persistence, ring spread, and cleanup. |
| `ebwizardry:mind_trick` | necromancy | novice | 10 / 0 / 40 | Restored with target suppression, attack-triggered release, and player nausea. |
| `ebwizardry:mine` | earth | novice | 5 / 0 / 5 | Restored with tier-limited mining, per-block protection, and Charm of Silk Touch. |
| `ebwizardry:mirage` | sorcery | advanced | 40 / 10 / 200 | Restored self buff, mob target suppression, and periodically shifted rendering. |
| `ebwizardry:muffle` | sorcery | novice | 5 / 0 / 25 | Restored with an entity-sound mute and original frontal mob detection arc. |
| `ebwizardry:paralysis` | lightning | advanced | 20 / 10 / 60 | Restored with the existing status effect, shock ray, shorter player duration, and critical-hit release. |
| `ebwizardry:petrify` | sorcery | advanced | 40 / 20 / 100 | Restored stone conversion and light-sensitive release after the minimum lifetime. |
| `ebwizardry:possession` | necromancy | master | 100 / 15 / 300 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:radiant_totem` | healing | advanced | 40 / 15 / 175 | Restored wounded-ally healing priority, hostile damage, rotating cube and ambient audio. |
| `ebwizardry:resurrection` | healing | master | 150 / 25 / 400 | Missing. No corresponding Redux spell registration. |
| `ebwizardry:shadow_ward` | necromancy | advanced | 10 / 0 / 0 | Restored channeled damage split and reflection. |
| `ebwizardry:shield` | healing | apprentice | 5 / 0 / 0 | Restored continuous Resistance, projectile deflection and animated shield. |
| `ebwizardry:silverfish_swarm` | earth | master | 80 / 20 / 300 | Enabled with dedicated non-infesting minions that multiply on kills through generation five. |
| `ebwizardry:sixth_sense` | earth | apprentice | 20 / 0 / 100 | Restored range-based creature sensing, shader and Dire Wolf Pelt shapes. |
| `ebwizardry:slow_time` | sorcery | master | 100 / 15 / 200 | Restored scoped tick cadence, player sync, shader and Stop Time charm. |
| `ebwizardry:snare` | earth | novice | 10 / 0 / 10 | Restored a single-use, floor-supported trap with saved ownership, damage, and Slowness III. |
| `ebwizardry:snowball` | ice | novice | 1 / 0 / 1 | Restored with a vanilla snowball and original ballistic range. |
| `ebwizardry:stormcloud` | lightning | advanced | 60 / 10 / 150 | Restored a cloud five blocks above the cast point with periodic shock arcs, original sounds and particles, and Ring of Stormcloud support. |
| `ebwizardry:summon_ice_giant` | ice | master | 100 / 20 / 400 | Previously development-only. Enabled for release using the existing giant and its original texture. |
| `ebwizardry:summon_ice_wraith` | ice | advanced | 40 / 10 / 200 | Restored under the original ID with the existing ice wraith entity. |
| `ebwizardry:summon_iron_golem` | sorcery | master | 175 / 20 / 400 | Restored as Summon Spectral Golem, with the original entity ID, texture, and 600-tick lifetime. |
| `ebwizardry:summon_phoenix` | fire | master | 150 / 20 / 400 | Restored original model, hovering minion, fire immunity, and channeled Flame Ray. |
| `ebwizardry:summon_shadow_wraith` | necromancy | master | 100 / 20 / 400 | Restored with the existing particle-based shadow wraith, Darkness Orb attack, and 600-tick lifetime. |
| `ebwizardry:summon_spirit_horse` | earth | advanced | 50 / 10 / 150 | Restored. Owned tamed mount with intrinsic saddle, potency scaling and a fade after 200 idle ticks. |
| `ebwizardry:summon_spirit_wolf` | earth | apprentice | 25 / 0 / 100 | Restored a persistent tamed companion with potency scaling, replacement, and wand dismissal. |
| `ebwizardry:summon_storm_elemental` | lightning | master | 100 / 20 / 400 | Restored with the existing particle-based storm elemental, its original seeking-disc attack, and 600-tick lifetime. |
| `ebwizardry:thunderstorm` | lightning | master | 100 / 20 / 250 | Restored ten owned outdoor lightning strikes with secondary and tertiary chaining. |
| `ebwizardry:transience` | healing | advanced | 50 / 15 / 100 | Restored invisibility, damage and interaction restrictions, and afterimage shader. |
| `ebwizardry:transportation` | sorcery | advanced | 100 / 20 / 100 | Restored craftable stones, saved circles, compass selection, timed travel and mount transport. |
| `ebwizardry:wall_of_frost` | ice | master | 15 / 15 / 70 | Restored channelled ice placement and mob freezing. |
| `ebwizardry:withering_totem` | necromancy | advanced | 50 / 20 / 200 | Restored damage, stored drain, expiry burst with Wither, and rotating cube. |

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

2 spells lack an available casting implementation. The two previously gated
original spells ship under their Redux IDs. Greater Telekinesis is release-playable
with a block-entity parity gap. That leaves 3 open entries among the 64 tracked
here. See [Restoration progress](../RESTORATION-PROGRESS.md) for the registry
counts and verification-only work.

Direct 1.12.2 save conversion and uninterrupted channel resumption after chunk unload
are outside the verified Redux save behavior. The lectern currently uses modern
widgets on the original book textures. Full visual, combat, and multiplayer
verification across all restored spells is still open.

## Test evidence

- The 103-test suites cover pausing a nearby projectile, the sixth-tick update, full pause from Stop Time, and cleanup following effect removal.

- The 102-test development and packaged suites cover reversible mob conversion and stored equipment, Petrify obstruction and upper-section release, and two-block Wall of Frost placement and dry thaw.

- The 99-test development and packaged suites cover Earthquake wave damage, Weakness, launch, protected terrain and settling, plus open and locked barrel refusal, no spill, saved landing, and exactly one recovery item.

- The 97-test development and packaged runs cover Decoy ownership, invulnerability, aggro diversion, and expiry, Mirage target suppression, and Phoenix hover and Flame Ray damage.

- The 94-test development run covers Mind Control target redirection, rejection of the controller as a target, persisted controller UUID, and expiry cleanup.

- The 93-test development run covers Grapple extension before pull, block anchor invalidation, and location-cast entity reeling.

- The 92-test development run covers Transience damage, entity interactions and effect cleanup, plus Sixth Sense duration and range upgrades.

- The 90-test development run covers Radiant Totem priority, Withering Totem persistence and burst, Clairvoyance range and dimension checks, thorn growth and paired expiry, and Black Hole attraction, core damage and expiry.

- The 85-test development run covers Disintegration fire damage and ignition, its single lethal ember burst, and ember contact ignition and expiration.

- The 84-test development run covers Shield resistance, arrow deflection and cleanup, plus Shadow Ward damage reflection and channel termination.

- The 82-test development and packaged runs cover Lightning Hammer landing, hard impact, timed shocks, pickup, ownership, lifetime and throw, plus the Lightning Web 5/4/3 damage chain.

- The development and packaged GameTest suites each passed 79 required tests. The new checks cover owned lightning, Stormcloud cadence and expiry, Snare triggering, Spirit Wolf replacement, and the Spirit Horse saddle and idle fade.

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
- The three-spell follow-up restored Paralysis, Summon Shadow Wraith,
  and Summon Storm Elemental. All 55 packaged GameTests passed. The three
  original spell definitions and icons were checked in the JAR. The client
  booted with the new look-lock mixin. Interactive verification of this
  batch’s visuals, audio, player movement, and multiplayer remains open.

## Remaining source and property index

Original spell classes are under the pinned checkout at src/main/java/electroblob/wizardry/spell. Generic registrations are in registry/Spells.java. This index identifies entry points and dependencies for investigation. The full multiplayer, visual, and safety audits for these spells are still open.

| Original ID | Original entry point | Original base properties | Porting dependencies |
| --- | --- | --- | --- |
| `ebwizardry:possession` | Possession.java | range=8, effect_duration=600, critical_health=1 | Transferred control, camera, original body and logout cleanup |
| `ebwizardry:resurrection` | Resurrection.java | effect_radius=8, wait_time=200 | Death interception, revival state and multiplayer safeguards |

### Forcefield and Transportation validation

- The two spells bring release availability to 186/188. The 105-test development and packaged suites check cross-boundary projectile, damage and explosion behavior, plus the 75-tick Transportation delay, broken-circle rejection and arrival effect.
- Interactive checks still include Forcefield visuals and audio, Transportation compass selection and mount travel, and remote client behavior.

### Earlier 64-test checkpoint

- NeoForge development and packaged runs each passed 64 required GameTests.
  Targeted coverage includes owned projectiles, saved spell item resolution,
  summon type and expiry, Silverfish kill multiplication, Mind Trick release,
  Celestial Smite area damage and fire, a protected Greater Fireball impact,
  occupied armor slots and conjuration cleanup, one-use damage amplification,
  and frost retaliation.
- The packaged dedicated server completed 100 ticks. The packaged client
  completed 360 ticks in its isolated smoke world. These boot checks did not
  visually verify the newly restored spells.
- The final NeoForge JAR contains 149 base spell definitions and the ten
  restored spell icons. Their original metadata and base values were checked.
- Fabric Java compilation passed. Interactive spell book and scroll use for
  the full batch, Creative search, multiplayer, audio, and visual fidelity
  remain open.

### Frost Barrier to Intimidate checkpoint

The NeoForge packaged and development suites reached 73 required GameTests.
They cover Frost Barrier placement, obstruction and expiry, the Imbue Weapon
and Freezing Weapon sword and bow effects, Mine’s harvest tiers and drops,
Divination’s strong-signal nudge, Chain Lightning’s separate damage tiers,
Muffle’s detection arc, Intimidate’s targeting and saved fear source, and
Greater Telekinesis shulker protection, landing, and recovery. The packaged
Frost Barrier screenshot was visually inspected. The other new spells still
need interactive visual and audio checks, and the full multiplayer matrix
remains open.