package com.binaris.wizardry.core.mixin.accessor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RandomizableContainerBlockEntity.class)
public interface RCBEAccessor {
    @Accessor
    net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> getLootTable();

    @Accessor
    void setLootTable(net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootTable);
}
