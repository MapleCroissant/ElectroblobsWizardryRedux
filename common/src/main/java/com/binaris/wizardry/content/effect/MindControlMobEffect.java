package com.binaris.wizardry.content.effect;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.content.entity.living.AbstractWizard;
import com.binaris.wizardry.core.AllyDesignation;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.setup.registries.EBItems;
import com.binaris.wizardry.setup.registries.EBMobEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import com.binaris.wizardry.api.content.effect.MagicMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

public final class MindControlMobEffect extends MagicMobEffect {
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, WizardryMainMod.location("mind_control_immune"));

    public interface ControllerAccess {
        @Nullable UUID ebwizardry$getController();
        void ebwizardry$setController(@Nullable UUID controller);
    }

    public MindControlMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x8626cb);
        hideVanillaParticles();
    }

    @Override
    public void spawnCustomParticle(net.minecraft.world.level.Level level, double x, double y, double z) {}

    public static boolean canControl(Entity target) {
        return target instanceof Mob mob && mob.isAlive() && !(mob instanceof Npc || mob instanceof AbstractWizard)
                && !mob.getType().is(IMMUNE);
    }

    public static boolean apply(Mob target, LivingEntity controller, int duration) {
        if (!canControl(target) || duration <= 0 || target.level().isClientSide) return false;
        if (!target.addEffect(new MobEffectInstance(EBMobEffects.MIND_CONTROL.get(), duration, 0, false, false), controller)) return false;
        ((ControllerAccess) target).ebwizardry$setController(controller.getUUID());
        retarget(target, controller);
        return true;
    }

    @Nullable
    public static LivingEntity controllingEntity(Mob mob) {
        var id = ((ControllerAccess) mob).ebwizardry$getController();
        return id != null && mob.level() instanceof ServerLevel level && level.getEntity(id) instanceof LivingEntity owner
                && owner.isAlive() ? owner : null;
    }

    private static boolean valid(Mob mob, LivingEntity controller, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || target == mob || target == mob.getVehicle()
                || target instanceof net.minecraft.world.entity.decoration.ArmorStand
                || target instanceof Player player && (player.isCreative() || player.isSpectator())
                || !AllyDesignation.isValidTarget(controller, target)) return false;
        return !(target instanceof Mob other) || !other.hasEffect(EBMobEffects.MIND_CONTROL.get())
                || !controller.getUUID().equals(((ControllerAccess) other).ebwizardry$getController());
    }

    @Nullable
    public static LivingEntity filterTarget(Mob mob, @Nullable LivingEntity intended) {
        if (intended == null || mob.level().isClientSide || !mob.hasEffect(EBMobEffects.MIND_CONTROL.get())) return intended;
        var controller = controllingEntity(mob);
        if (controller == null) return null;
        return valid(mob, controller, intended) ? intended : nearest(mob, controller);
    }

    @Nullable
    private static LivingEntity nearest(Mob mob, LivingEntity controller) {
        double range = Math.clamp(mob.getAttributeValue(Attributes.FOLLOW_RANGE), 0, 64);
        return EntityUtil.getLivingWithinRadius(range, mob.getX(), mob.getY(), mob.getZ(), mob.level()).stream()
                .filter(target -> valid(mob, controller, target)).min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
    }

    private static void retarget(Mob mob, @Nullable LivingEntity controller) {
        var target = controller == null ? null : nearest(mob, controller);
        mob.setTarget(target);
        var brain = mob.getBrain();
        if (brain.checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
            if (target == null) brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            else brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Mob mob) || entity.level().isClientSide || !canControl(mob)) return true;
        var owner = controllingEntity(mob);
        var brain = mob.getBrain();
        if (brain.checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT)) {
            var intended = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
            var allowed = filterTarget(mob, intended);
            if (allowed != intended) {
                if (allowed == null) brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                else brain.setMemory(MemoryModuleType.ATTACK_TARGET, allowed);
            }
        }
        if (mob.tickCount % 50 != 0) return true;
        if (owner == null || !valid(mob, owner, mob.getTarget())) retarget(mob, owner);
        if (owner instanceof Player player && ArtifactChannel.isEquipped(player, EBItems.RING_MIND_CONTROL.get())) {
            var effect = mob.getEffect(EBMobEffects.MIND_CONTROL.get());
            if (effect == null) return true;
            for (var nearby : EntityUtil.getLivingWithinRadius(3, mob.getX(), mob.getY(), mob.getZ(), mob.level())) {
                if (nearby instanceof Mob candidate && canControl(candidate) && valid(mob, owner, nearby)
                        && mob.getRandom().nextInt(10) == 0) {
                    apply(candidate, owner, effect.getDuration());
                }
            }
        }
        return true;
    }

    public static void clear(Mob mob) {
        ((ControllerAccess) mob).ebwizardry$setController(null);
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        if (mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }
}
