# Fabric 1.21.1 API migration

This branch targets Minecraft 1.21.1, Fabric and Java 21. The preserved Forge
1.20.1 implementation is excluded from Gradle settings; it is not a 1.21 port.
Existing gameplay registration names are retained.

- Effects and attributes use `Holder<MobEffect>` and `Holder<Attribute>`.
  Enchantments use dynamic registry keys/holders, resolved with a level's registry
  access. Banner patterns are data-driven registry entries.
- Item NBT is represented by native data components. `ItemData.read()` returns
  a snapshot; change values with `ItemData.update()` or `set()`. Conjure and
  temporary enchantment state use native components instead of the removed CCA
  item module. Entity/block CCA implementations accept registry lookup providers.
- `WizardryAttributeModifier` is a factory, since native `AttributeModifier` is
  now a final record. UUIDs map deterministically to
  `ebwizardry:modifier/<uuid>`; conditions are attached through
  `ConditionalAttributeModifier`. The command's name argument is retained for
  syntax compatibility; native modifier identity is its resource location.
- Wizard armor materials wrap the new registered material records; the existing
  wizard-specific durability, defense, mana and upgrade behavior is retained.
- Custom recipes use `RecipeInput`, registry-aware item serialization,
  `MapCodec` and `StreamCodec`. Existing imbuement JSON fields remain supported.
- Messages implement native typed custom payloads; existing channel IDs remain.
  Clients and servers must both use this port's protocol.
- Resource registry directories use Minecraft 1.21's singular names, including
  `recipe`, `advancement`, `loot_table`, `structure`, `tags/item`, `tags/block`.
  External resource/data packs and add-ons must be ported separately.

Back up worlds before upgrading. Native data fixing handles vanilla inventory
conversion; explicit adapters cover mod-owned item fields and wizard trades.
This is not a guarantee that every historical mod/world combination upgrades.

## NeoForge 1.21.1

The independent `neoforge/` module compiles the same common sources with Java 21
and official runtime names. Its service providers use native registries,
attachments and typed payloads; it has no Fabric API or CCA runtime requirement.
Player attachments persist and copy on death; entity/block attachment updates
are synchronized to tracking players. Retained legacy CCA/Forge fields can be
imported without overriding already-saved native attachments.

NeoForge registers commands and advancement triggers at loader events, and
resynchronizes existing magic effects after the entity spawn bundle. Its patched
living-entity durability overload is handled separately to retain unbreakable
wizard armor and conjured items. Fabric keeps its existing paths.

Optional Curios uses the live equipped inventory for immediate artifact removal;
Accessories remains the preferred integration when installed. NeoForge-specific
JEI categories show altar recipes, weighted outputs, workbench spell binding and
tier upgrades; item subtypes distinguish spell-bearing books, scrolls and wands.
