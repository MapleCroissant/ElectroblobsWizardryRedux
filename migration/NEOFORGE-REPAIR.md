# NeoForge 1.21.1 repair and addon API

For the later spell comparison, lectern restoration, and base scroll dispenser, see the [original spell inventory](ORIGINAL-SPELL-INVENTORY.md). The audit below records the earlier rendering and API pass.

The original comparison uses Electroblob's Wizardry 4.3.19 at 1.12.2 commit
[c0be747](https://github.com/Electroblob77/Wizardry/tree/c0be7475b234223e5a3276e62e38e69ffa6de0ce).
Redux uses NeoForge 21.1.266 and Java 21.

## Rendering and feature audit

| Area | Result |
| --- | --- |
| Iceball and shared projectiles | Iceball uses the original 0.7-scale camera-facing texture. Seven projectile registrations share the repaired buffered renderer. Firebolt, Spark, Ice Charge, and Darkness Orb regain their original scales, and Spark regains alpha blending. The Magic Fireball texture is the original fireball texture under Redux's resource name. |
| Frost Ray and shared rays | The original effect consists of moving sparkle and snow particles at half-block intervals. Wand and continuous scroll casts now replay accepted ticks with the correct tick index. Ray effects use the actual entity intercept and distinguish misses from block hits. Successful location casts now return success. Frost Breath belongs to Twilight Forest Spell Pack. |
| Particle defaults | The shared particle billboard now follows vanilla winding. Frost Ray created particles before the fix, but its reversed quads were culled. Independent builders also initialize their defaults before the first spawn. |
| Books, scrolls and discovery | Book item use, workbench binding, blank-scroll inscription, glyph masking and persisted discovery exist. The book screen now draws its background once per frame, fixing the repeated blur and redraw. Base scroll dispensers are still missing. Twilight Forest provides its own scroll dispenser. |
| Spell index | The original base mod has a lectern with nearby bookshelf indexing, search, sort, held spell selection, and a shelf locator. Redux's six lecterns only implement block placement and rotation. Twilight Forest has its own lectern implementation. Restoring the base index still needs the bookshelf query, server-validated selection, screen, and book renderer. |
| Wand systems | Tier and elemental upgrades, mana storage, workbench recharge, condenser regeneration, siphon, and cooldown storage are present. Channel affordability and release accounting still use Redux's existing formulas, including its different continuous wand and scroll cooldown bases. |
| Casting and elemental effects | Registry and data-pack definitions exist for the implemented spells. The spell list comparison below identifies original entries without direct ID matches. Effects, elemental immunities, summons and construct behavior still need individual gameplay comparison. |
| HUD, effects, audio and animations | The HUD, spell actions, particle providers and loop sound paths exist. Original shadow wraiths, storm elementals, blizzards, tornadoes and hailstorms also used blank entity renderers and rely on separate visual effects. Their effect parity is unverified. Other immediate-mode renderers need their own visual checks. |
| Dedicated and multiplayer | Packaged testing exercises loader startup, registry metadata, server-side completion, client replay, and the graphical render path. Interactive play and two-player packet tracking remain separate checks. |

The original asset tree has 189 spell definitions. Redux's baseline generated
data has 125 spell files, with 123 exact filename matches. These file counts include sentinels and development-only content. Redux's IDs `dark_vision` and `fire_skin` correspond
to original `darkvision` and `fireskin`. Keep Redux's existing IDs stable.

The other 64 original IDs without direct matches were

- `black_hole`, `blink`, `celestial_smite`, `chain_lightning`, `clairvoyance`,
  `conjure_armour`, `decoy`, `diamondflesh`, `disintegration`, `divination`,
  `earthquake`, `empowering_presence`, `forcefield`, `forest_of_thorns`,
  `freezing_weapon`, `frost_barrier`, `glide`, `grapple`, `greater_fireball`,
  `greater_telekinesis`, `ice_shroud`, `ice_statue`, `imbue_weapon`, `intimidate`,
  `ironflesh`, `light`, `lightning_bolt`, `lightning_disc`, `lightning_hammer`,
  `lightning_pulse`, `lightning_ray`, `lightning_web`, `mark_sacrifice`,
  `mind_control`, `mind_trick`, `mine`, `mirage`, `muffle`, `paralysis`,
  `petrify`, `possession`, `radiant_totem`, `resurrection`, `shadow_ward`,
  `shield`, `silverfish_swarm`, `sixth_sense`, `slow_time`, `snare`, `snowball`,
  `stormcloud`, `summon_ice_giant`, `summon_ice_wraith`, `summon_iron_golem`,
  `summon_phoenix`, `summon_shadow_wraith`, `summon_spirit_horse`,
  `summon_spirit_wolf`, `summon_storm_elemental`, `thunderstorm`, `transience`,
  `transportation`, `wall_of_frost`, and `withering_totem`.

## Arcane Ascension integration

### Identity and experience

Read `spell.getLocation()`, `spell.getElement().getLocation()`, and
`spell.getTier().getLocation()` after registries and data packs load. Use these
namespaced resource locations for storage and lookup. The spell ID identifies
the registered spell, while element and tier reflect its effective data-pack
configuration. `RegistryUtils.getSpells(predicate)` enumerates registered spells
across namespaces and excludes `Spells.NONE`. A NeoForge GameTest checks that
the entire runtime spell registry round-trips through all three lookup APIs.

Register one `SpellCastEvent.Completed` listener on `WizardryEventBus`. It
fires on the logical server for successful instant wand or scroll casts and
for successful channels on their normal release or natural scroll completion.
Charging and releasing before a successful cast gives no completion. Repeated
successful casts of the same spell each emit their own event. A channel discarded
without its normal finish callback gives no completion.

```java
WizardryEventBus.getInstance().register(SpellCastEvent.Completed.class, event -> {
    if (!(event.getCaster() instanceof ServerPlayer player)
            || event.getLevel().isClientSide || event.getSpell() == Spells.NONE) return;

    var spell = event.getSpell();
    var spellId = spell.getLocation();
    var elementId = spell.getElement().getLocation();
    var tierId = spell.getTier().getLocation();
    // Give the player general XP derived from tierId, and elemental XP for elementId.
});
```

`Pre` is cancelable and can occur while an attempt is still pending. `Tick`
describes a channel tick. `Post` is the successful-start notification for
normal item casts and also occurs in client replay, command, and NPC paths.
`Finish` is a cleanup notification and also fires after unsuccessful charging.
Use one event stream for XP to avoid double counting.

The new item hook uses the active `Spell` instance, so Twilight Forest Spell
Pack and Extra Spells spells automatically participate when bound to standard
wands or used through standard scrolls. Commands and NPC goals have their own
events. A direct call to `Spell.cast(...)` bypasses the item event pipeline.
Twilight Forest's dispenser invokes its own location-cast pipeline, with no
player to award. Custom player-casting items can post `Completed` once on the
server at their own successful finish point. Some existing bypasses include
direct spell calls inside effects, tests and client replays. An accepted regular
wand or scroll spell still produces one event if it internally casts helper
spells.

### Stats

| Stat | Integration |
| --- | --- |
| Mana cost | In `SpellCastEvent.Prepare`, apply `multiplyTotal(SpellModifiers.COST, factor)` to the item-use modifiers. This affects initial affordability, charged casts and the channel's cost snapshot. `Pre` is still available for one-off cast adjustments. For repeated channel ticks, avoid multiplying the same mutable context on every `Tick`. |
| Cooldown | Use `SpellCastEvent.Cooldown.multiply(factor)` on the resolved cooldown. It runs immediately before a wand or scroll applies the cooldown and supports both instant spells and channels. Use this or the existing instant `SpellModifiers.COOLDOWN` route for a given bonus to avoid applying it twice. Channel cooldowns retain their current bases. A wand channel uses its mana-cost-derived base, so changing `COST` also affects that base. |
| Potency and range | Multiply `POTENCY` and `RANGE` in `Prepare`, or per cast using existing modifier-aware spell hooks. Support depends on the spell applying those keys. Permanent player attributes also exist as `EBAttributes.CAST_*`, but they are converted in `Pre`. Use `Prepare` for continuous item-use modifiers. |
| Casting speed | Multiply `CHARGEUP` in `Prepare` before the wand captures the duration. Register the listener on both sides and synchronize any player data it reads. For example, a duration factor of 0.8 means 20 percent fewer charge ticks. Scrolls retain their own use durations. |
| Effective capacity | Update `DataComponents.MAX_DAMAGE` on the server-side wand stack. `WandItem.getManaCapacity` reads this component and still applies the existing storage upgrade. Calculate from a stable item baseline, then preserve and clamp the previous absolute mana through `setMana`. The ordinary stack sync handles the client. Reapply or remove the component when a wand changes owner or is replaced during a tier upgrade, since this is stack data. A live player-only capacity override would need an ownership policy across inventory, workbench and unowned stacks. |
| Regeneration | Register `WandManaRegenEvent` to modify the recharge amount on the server`s condenser interval for a player's depleted wands. A wand without a condenser has a zero base. The final recharge is capped at capacity. |
| Magical resistance | On NeoForge, subscribe to `LivingIncomingDamageEvent` and adjust the damage amount for the player when `EBDamageSources.isMagic(source)` matches. That helper covers Redux's registered magic types and vanilla `magic`. Extend the predicate or register an addon damage type as appropriate. A general damage event already provides this extension point. |

`Prepare` and `Cooldown` callbacks should read synchronized data and mutate
only the event. Use finite factors, apply matching elemental bonuses from the
event`s spell element, and avoid side effects in these calculation hooks.
Progression values, XP curves, player storage and passives belong in the addon.
`Completed` signals resolved cast success and does not imply an eventual
projectile hit or a persistent construct's expiry.

Use the same repaired Redux build on the client and server. Playback packets
carry the channel tick for continuous casts.

## Verification

- NeoForge build and packaged Redux GameTests passed all 44 required tests.
  The same suite passed with Curios, Accessories, and with both spell addons
  and Twilight Forest present. Fabric shared-source compilation passed.
- The packaged dedicated server started, ticked 100 times, and saved cleanly.
  The packaged graphical client entered an integrated world, received Iceball,
  channeled Frost Ray through normal item use, and opened the spell book.
- The captured frames show the projectile and the ray effect in game.
  Find them under `neoforge/run-packaged-smoke/screenshots` as
  `repair-iceball.png` and `repair-frost-ray.png`. Gameplay coverage is
  scripted, with manual inspection of the resulting frames.
- The JAR contains all 21 entity textures referenced by literal paths in
  `EBRenderers` and the snow and sparkle sprite dependencies. It contains
  icons at 187 of the 189 original spell paths plus the two renamed icons.
  A packaged icon can exist for a spell that still needs implementation.
- Extra Spells' dedicated-server check verified registration and casting of all
  18 spells. Twilight Forest's dedicated check verified all 33 metadata
  records in the combined profile.

- Twilight Forest's full gameplay verification passed in a copy of its own
  smoke world with the repaired Redux JAR. The baseline JAR passed in an
  independent copy of the same world. The portal assertion had failed when
  run in Redux's generic smoke world, so the gameplay result is scoped to
  the addon's fixture.
