package com.binaris.wizardry.content.spell.sorcery;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.api.content.spell.SpellAction;
import com.binaris.wizardry.api.content.spell.SpellTypes;
import com.binaris.wizardry.api.content.spell.internal.PlayerCastContext;
import com.binaris.wizardry.api.content.spell.internal.SpellModifiers;
import com.binaris.wizardry.api.content.spell.properties.SpellProperties;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.setup.registries.Elements;
import com.binaris.wizardry.setup.registries.SpellTiers;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class Divination extends Spell {
    private static final String MESSAGE = "spell.ebwizardry.divination.";
    private static final TagKey<Block> DIVINATION_ORES = TagKey.create(Registries.BLOCK, WizardryMainMod.location("divination_ores"));

    @Override
    public boolean cast(PlayerCastContext context) {
        var caster = context.caster();
        var level = context.world();
        double range = Math.clamp(context.modifiers().get(SpellModifiers.RANGE, property(DefaultProperties.RANGE)), 0, 32);
        double potency = context.modifiers().getFactor(SpellModifiers.POTENCY) - 1;
        BlockPos center = caster.blockPosition();
        int reach = (int) Math.ceil(range);
        double bestWeight = 0;
        BlockPos best = null;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) continue;
            double distance = caster.position().distanceTo(Vec3.atCenterOf(pos));
            if (distance > range || range == 0) continue;
            var state = level.getBlockState(pos);
            if (!state.is(DIVINATION_ORES)) continue;
            double weight = 1 - distance / range + 0.2 * potency * oreValue(state);
            if (weight > bestWeight) {
                bestWeight = weight;
                best = pos.immutable();
            }
        }

        if (best == null) {
            if (!level.isClientSide) caster.sendSystemMessage(Component.translatable(MESSAGE + "nothing"));
        } else {
            Vec3 delta = Vec3.atCenterOf(best).subtract(caster.getEyePosition());
            Direction direction = Direction.getNearest(delta.x, delta.y, delta.z);
            int tier = bestWeight > 0.75 ? 4 : bestWeight > 0.5 ? 3 : bestWeight > 0.25 ? 2 : 1;
            if (!level.isClientSide) {
                String signal = switch (tier) {
                    case 4 -> "very_strong";
                    case 3 -> "strong";
                    case 2 -> "moderate";
                    default -> "weak";
                };
                caster.sendSystemMessage(Component.translatable(MESSAGE + signal,
                        Component.translatable(MESSAGE + relativeName(direction, caster.getDirection()))));
                if (tier == 4) {
                    caster.push(new Vec3(direction.step()).scale(0.2));
                    if (caster instanceof ServerPlayer serverPlayer) {
                        serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(caster));
                    }
                }
            } else if (tier > 1) {
                Vec3 hint = Vec3.atCenterOf(center.above().relative(direction, 2));
                int count = tier == 4 ? 12 : tier == 3 ? 8 : 3;
                for (int i = 0; i < count; i++) {
                    ParticleBuilder.create(EBParticles.FLASH, level.random, hint.x, hint.y, hint.z, 0.7, false)
                            .time(20 + level.random.nextInt(5))
                            .color(0.6f + level.random.nextFloat() * 0.4f, 0.6f + level.random.nextFloat() * 0.4f,
                                    0.6f + level.random.nextFloat() * 0.4f).scale(0.3f).spawn(level);
                }
            }
        }
        playSound(level, caster, context.castingTicks(), -1);
        return true;
    }

    private static String relativeName(Direction direction, Direction facing) {
        if (!direction.getAxis().isHorizontal()) return direction.getName();
        if (direction == facing) return "front";
        if (direction == facing.getOpposite()) return "back";
        return direction == facing.getClockWise() ? "right" : "left";
    }

    private static double oreValue(BlockState state) {
        if (state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES)) return 5;
        if (state.is(BlockTags.LAPIS_ORES) || state.is(BlockTags.REDSTONE_ORES)) return 3;
        if (state.is(BlockTags.IRON_ORES) || state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.COPPER_ORES)) return 2.8;
        if (state.is(BlockTags.COAL_ORES)) return 1;
        return 4;
    }

    @Override
    protected SpellProperties properties() {
        return SpellProperties.builder()
                .assignBaseProperties(SpellTiers.APPRENTICE, Elements.SORCERY, SpellTypes.UTILITY, SpellAction.THRUST, 35, 0, 80)
                .add(DefaultProperties.RANGE, 8f).build();
    }
}
