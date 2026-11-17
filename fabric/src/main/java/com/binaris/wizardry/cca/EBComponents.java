package com.binaris.wizardry.cca;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.cca.blockentity.ArcaneLockDataHolder;
import com.binaris.wizardry.cca.entity.ContainmentDataHolder;
import com.binaris.wizardry.cca.entity.MinionDataHolder;
import com.binaris.wizardry.cca.player.CastCommandDataHolder;
import com.binaris.wizardry.cca.player.SpellManagerDataHolder;
import com.binaris.wizardry.cca.player.WizardDataHolder;
import org.ladysnake.cca.api.v3.block.BlockComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.block.BlockComponentInitializer;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

/**
 * Cardinal Components entry point for entity and block data. Items use vanilla components.
 */
public class EBComponents implements EntityComponentInitializer, BlockComponentInitializer {
    public static final ComponentKey<WizardDataHolder> WIZARD_DATA = ComponentRegistry.getOrCreate(WizardryMainMod.location("wizard_data"), WizardDataHolder.class);
    public static final ComponentKey<CastCommandDataHolder> CAST_COMMAND_DATA = ComponentRegistry.getOrCreate(WizardryMainMod.location("cast_command_data"), CastCommandDataHolder.class);
    public static final ComponentKey<SpellManagerDataHolder> SPELL_MANAGER_DATA = ComponentRegistry.getOrCreate(WizardryMainMod.location("spell_manager_data"), SpellManagerDataHolder.class);

    public static final ComponentKey<MinionDataHolder> MINION_DATA = ComponentRegistry.getOrCreate(WizardryMainMod.location("minion_data"), MinionDataHolder.class);
    public static final ComponentKey<ContainmentDataHolder> CONTAINMENT_DATA = ComponentRegistry.getOrCreate(WizardryMainMod.location("containment_data"), ContainmentDataHolder.class);

    public static final ComponentKey<ArcaneLockDataHolder> ARCANE_LOCK_DATA = ComponentRegistry.getOrCreate(WizardryMainMod.location("arcane_lock"), ArcaneLockDataHolder.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(WIZARD_DATA, WizardDataHolder::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(CAST_COMMAND_DATA, CastCommandDataHolder::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(SPELL_MANAGER_DATA, SpellManagerDataHolder::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerFor(Mob.class, MINION_DATA, MinionDataHolder::new);
        registry.registerFor(LivingEntity.class, CONTAINMENT_DATA, ContainmentDataHolder::new);
    }

    @Override
    public void registerBlockComponentFactories(BlockComponentFactoryRegistry blockComponentFactoryRegistry) {
        blockComponentFactoryRegistry.registerFor(BaseContainerBlockEntity.class, ARCANE_LOCK_DATA, ArcaneLockDataHolder::new);
    }
}
