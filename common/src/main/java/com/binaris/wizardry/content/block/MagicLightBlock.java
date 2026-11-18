package com.binaris.wizardry.content.block;

import com.binaris.wizardry.api.content.item.ICastItem;
import com.binaris.wizardry.content.blockentity.MagicLightBlockEntity;
import com.binaris.wizardry.core.integrations.ArtifactChannel;
import com.binaris.wizardry.setup.registries.EBBlockEntities;
import com.binaris.wizardry.setup.registries.EBItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class MagicLightBlock extends Block implements EntityBlock {
    public MagicLightBlock() {
        super(Properties.of().noCollission().noOcclusion().noLootTable().strength(-1, 0)
                .pushReaction(PushReaction.BLOCK).lightLevel(state -> 15));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entity && entity.getEntity() instanceof Player player
                && (player.getMainHandItem().getItem() instanceof ICastItem || player.getOffhandItem().getItem() instanceof ICastItem)
                && ArtifactChannel.isEquipped(player, EBItems.CHARM_LIGHT.get())) return Shapes.block();
        return Shapes.empty();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof ICastItem) || !ArtifactChannel.isEquipped(player, EBItems.CHARM_LIGHT.get())
                || !level.mayInteract(player, pos)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) level.removeBlock(pos, false);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicLightBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != EBBlockEntities.MAGIC_LIGHT.get()) return null;
        return (world, pos, block, entity) -> ((MagicLightBlockEntity) entity).tick(world);
    }
}
