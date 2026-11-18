package com.binaris.wizardry.content.entity.construct;

import com.binaris.wizardry.api.content.entity.construct.ScaledConstructEntity;
import com.binaris.wizardry.setup.registries.EBSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class IceBarrierConstruct extends ScaledConstructEntity {
    private static final double THICKNESS = 0.4;

    private int delay = 0;

    public IceBarrierConstruct(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
        this.setBaseSize(1.8f, 1.05f);
    }

    public void setDelay(int delay) {
        this.delay = delay;
        this.setLifetime(this.lifetime + delay);
    }

    @Override
    public void setRot(float yaw, float pitch) {
        super.setRot(yaw, pitch);
        updateBarrierBounds();
    }

    private void updateBarrierBounds() {
        float a = Mth.abs(Mth.cos((float) Math.toRadians(getYRot())));
        float b = Mth.abs(Mth.sin((float) Math.toRadians(getYRot())));
        double x = getBbWidth() / 2 * a + THICKNESS * sizeMultiplier / 2 * b;
        double z = getBbWidth() / 2 * b + THICKNESS * sizeMultiplier / 2 * a;
        setBoundingBox(new AABB(this.getX() - x, this.getY(), this.getZ() - z, this.getX() + x, this.getY() + getBbHeight(), this.getZ() + z));
    }

    @Override
    public boolean isPickable() { return !isRemoved(); }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void tick() {
        if (level().isClientSide && firstTick) {
            setSizeMultiplier(sizeMultiplier);
            setRot(getYRot(), getXRot());
        }

        this.xOld = getX();
        this.yOld = getY();
        this.zOld = getZ();

        if (!level().isClientSide) {
            double extensionSpeed = 0;

            if (lifetime - this.tickCount < 20) {
                extensionSpeed = -0.01 * (this.tickCount - (lifetime - 20)) * sizeMultiplier;
            } else if (tickCount > 3 + delay) {
                extensionSpeed = 0;
            } else if (tickCount > delay) {
                extensionSpeed = 0.5 * sizeMultiplier;
            }

            this.move(MoverType.SELF, new Vec3(0, extensionSpeed, 0));
        }

        updateBarrierBounds();
        if (!level().isClientSide && tickCount == delay + 1) this.playSound(EBSounds.ENTITY_ICE_BARRIER_EXTEND.get(), 1, 1.5f);

        super.tick();

        Vec3 look = this.getLookAngle();

        if (!level().isClientSide) {
            for (Entity entity : level().getEntities(this, getBoundingBox().inflate(2))) {
                if (entity instanceof ScaledConstructEntity) continue;

                if (!entity.getBoundingBox().intersects(this.getBoundingBox())) continue;

                double perpendicularDist = getSignedPerpendicularDistance(entity.position());

                if (Math.abs(perpendicularDist) < entity.getBbWidth() / 2 + THICKNESS / 2) {
                    double velocity = 0.25 * (perpendicularDist >= 0 ? 1 : -1);
                    entity.push(velocity * look.x, 0, velocity * look.z);

                    if (entity instanceof ServerPlayer) {
                        ((ServerPlayer) entity).connection.send(new ClientboundSetEntityMotionPacket(entity));
                    }
                }
            }
        }

    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        this.playSound(EBSounds.ENTITY_ICE_BARRIER_DEFLECT.get(), 0.7f, 2.5f);
        return super.hurt(source, amount);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        delay = nbt.getInt("delay");
        tickCount = nbt.getInt("barrier_age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("delay", delay);
        nbt.putInt("barrier_age", tickCount);
    }

    public java.util.Optional<Vec3> intersectRay(Vec3 origin, Vec3 end, double margin) {
        double first = getSignedPerpendicularDistance(origin);
        double last = getSignedPerpendicularDistance(end);
        double difference = first - last;
        if (Math.abs(difference) < 1.0e-6) return java.util.Optional.empty();
        double travel = first / difference;
        if (travel < 0 || travel > 1) return java.util.Optional.empty();
        Vec3 impact = origin.lerp(end, travel);
        return getBoundingBox().inflate(margin).contains(impact) ? java.util.Optional.of(impact) : java.util.Optional.empty();
    }

    private double getSignedPerpendicularDistance(Vec3 point) {
        Vec3 look = this.getLookAngle();
        Vec3 delta = new Vec3(point.x - this.getX(), 0, point.z - this.getZ());
        return delta.dot(look);
    }
}
