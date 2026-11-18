package com.binaris.wizardry.content.spell.necromancy;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.CastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.api.content.util.EntityUtil;
import com.binaris.wizardry.content.effect.MindControlMobEffect;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.content.spell.abstr.RaySpell;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class MindControl extends RaySpell {
    @Override
    public boolean canCastByEntity() { return false; }

    @Override
    public boolean canCastByLocation() { return false; }

    @Override
    protected boolean onEntityHit(CastContext context, EntityHitResult hit, Vec3 origin) {
        if (!(hit.getEntity() instanceof LivingEntity target) || context.caster() == null) return false;
        var level = context.world();
        var caster = context.caster();
        if (level.isClientSide) {
            for (int i = 0; i < 10; i++) {
                ParticleBuilder.create(EBParticles.DARK_MAGIC, level.random, target.getX(), target.getEyeY(), target.getZ(), 0.25)
                        .color(0.8f, 0.2f, 1).spawn(level);
                ParticleBuilder.create(EBParticles.DARK_MAGIC, level.random, target.getX(), target.getEyeY(), target.getZ(), 0.25)
                        .color(0.2f, 0.04f, 0.25f).spawn(level);
            }
            return true;
        }
        if (!(target instanceof Mob mob) || !MindControlMobEffect.canControl(mob)) {
            if (caster instanceof Player player) {
                player.displayClientMessage(Component.translatable("spell.resist", target.getName(), getDescriptionFormatted()), true);
            }
            return true;
        }
        if (mob instanceof Sheep sheep && sheep.getColor() == DyeColor.BLUE && EntityUtil.canDamageBlocks(caster, level)) {
            sheep.setColor(DyeColor.RED);
            caster.playSound(SoundEvents.EVOKER_PREPARE_WOLOLO, 1, 1);
        }
        MindControlMobEffect.apply(mob, caster, (int) context.modifiers().get(SpellModifiers.DURATION, property(DefaultProperties.EFFECT_DURATION)));
        return true;
    }

    @Override
    protected boolean onBlockHit(CastContext context, BlockHitResult hit, Vec3 origin) { return false; }

    @Override
    protected boolean onMiss(CastContext context, Vec3 origin, Vec3 direction) { return false; }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.ADVANCED, Elements.NECROMANCY, SpellTypes.ATTACK, SpellAction.POINT, 40, 10, 150)
                .add(DefaultProperties.RANGE, 8f).add(DefaultProperties.EFFECT_DURATION, 600).build();
    }
}
