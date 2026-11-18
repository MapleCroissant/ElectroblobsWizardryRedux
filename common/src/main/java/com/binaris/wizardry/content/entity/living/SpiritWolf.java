package com.binaris.wizardry.content.entity.living;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.item.ICastItem;
import com.binaris.wizardry.content.spell.earth.SummonSpiritWolf;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBSounds;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class SpiritWolf extends Wolf {
    private static final EntityDataAccessor<Integer> FADE = SynchedEntityData.defineId(SpiritWolf.class, EntityDataSerializers.INT);

    public SpiritWolf(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FADE, 0);
    }

    public float opacity() {
        return 1 - Math.min(10, entityData.get(FADE)) / 10f;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            ParticleBuilder.create(EBParticles.DUST).pos(getRandomX(1), getRandomY() + 0.2, getRandomZ(1))
                    .color(0.8f, 0.8f, 1).shaded(true).spawn(level());
            if (tickCount == 1) {
                for (int i = 0; i < 15; i++) {
                    ParticleBuilder.create(EBParticles.SPARKLE).pos(getRandomX(1), getRandomY() + 0.2, getRandomZ(1))
                            .color(0.8f, 0.8f, 1).spawn(level());
                }
            }
        } else if (entityData.get(FADE) > 0) {
            int age = entityData.get(FADE) + 1;
            if (age > 10) discard();
            else entityData.set(FADE, age);
        } else if (tickCount % 20 == 1 && getOwnerUUID() != null && level() instanceof ServerLevel server) {
            var owner = server.getServer().getPlayerList().getPlayer(getOwnerUUID());
            if (owner != null) {
                var active = Services.OBJECT_DATA.getSpellManagerData(owner).getVariable(SummonSpiritWolf.WOLF_UUID);
                if (!getUUID().equals(active)) discard();
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var item = player.getItemInHand(hand);
        if (isOwnedBy(player) && player.isShiftKeyDown() && item.getItem() instanceof ICastItem) {
            if (tickCount > 20 && !level().isClientSide && entityData.get(FADE) == 0) {
                entityData.set(FADE, 1);
                playSound(EBSounds.ENTITY_SPIRIT_WOLF_VANISH.get(), 0.7f, 1 + random.nextFloat() * 0.4f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (item.is(Items.WOLF_ARMOR)) return InteractionResult.PASS;
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean canMate(Animal other) { return false; }

    @Override
    protected boolean shouldDropLoot() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("WizardryDispel", entityData.get(FADE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FADE, Math.clamp(tag.getInt("WizardryDispel"), 0, 10));
    }
}
