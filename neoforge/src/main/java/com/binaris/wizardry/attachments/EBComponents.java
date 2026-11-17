package com.binaris.wizardry.attachments;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.attachments.player.*;
import com.binaris.wizardry.attachments.entity.*;
import com.binaris.wizardry.attachments.blockentity.ArcaneLockDataHolder;
import com.binaris.wizardry.network.AttachmentSyncS2C;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.*;
import net.neoforged.neoforge.registries.*;

import java.util.function.Function;
import java.util.function.Supplier;

/** Native, persistent NeoForge attachments with explicit tracking synchronization. */
public final class EBComponents {
    private static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, WizardryMainMod.MOD_ID);
    public static final Key<WizardDataHolder> WIZARD_DATA = register("wizard_data", holder -> new WizardDataHolder((Player) holder));
    public static final Key<CastCommandDataHolder> CAST_COMMAND_DATA = register("cast_command_data", holder -> new CastCommandDataHolder((Player) holder));
    public static final Key<SpellManagerDataHolder> SPELL_MANAGER_DATA = register("spell_manager_data", holder -> new SpellManagerDataHolder((Player) holder));
    public static final Key<MinionDataHolder> MINION_DATA = register("minion_data", holder -> new MinionDataHolder((Mob) holder));
    public static final Key<ContainmentDataHolder> CONTAINMENT_DATA = register("containment_data", holder -> new ContainmentDataHolder((LivingEntity) holder));
    public static final Key<ArcaneLockDataHolder> ARCANE_LOCK_DATA = register("arcane_lock", holder -> new ArcaneLockDataHolder((net.minecraft.world.level.block.entity.BaseContainerBlockEntity) holder));
    public static final java.util.List<Key<?>> ALL = java.util.List.of(WIZARD_DATA, CAST_COMMAND_DATA, SPELL_MANAGER_DATA, MINION_DATA, CONTAINMENT_DATA, ARCANE_LOCK_DATA);

    public static void register(IEventBus bus) { TYPES.register(bus); }

    private static <T extends PersistentData> Key<T> register(String id, Function<IAttachmentHolder,T> factory) {
        var type = TYPES.register(id, () -> AttachmentType.builder(factory).serialize(new IAttachmentSerializer<CompoundTag,T>() {
            @Override public T read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider registries) {
                T value = factory.apply(holder);
                value.readFromNbt(tag, registries);
                return value;
            }
            @Override public CompoundTag write(T value, HolderLookup.Provider registries) {
                CompoundTag tag = new CompoundTag();
                value.writeToNbt(tag, registries);
                return tag;
            }
        }).copyOnDeath().build());
        return new Key<>(id, type);
    }

    public record Key<T extends PersistentData>(String id, Supplier<AttachmentType<T>> type) {
        public T get(IAttachmentHolder holder) { return holder.getData(type); }
        public void sync(IAttachmentHolder holder) {
            if (holder instanceof Entity entity && !entity.level().isClientSide()) {
                AttachmentSyncS2C.send(entity, this);
            } else if (holder instanceof BlockEntity block && block.getLevel() != null && !block.getLevel().isClientSide()) {
                block.setChanged();
                AttachmentSyncS2C.send(block, this);
            }
        }
    }
}
