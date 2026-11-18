package com.binaris.wizardry.client;

import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ClairvoyanceParticles {
    private ClairvoyanceParticles() {}

    public static void spawn(List<BlockPos> points, int duration) {
        var level = Minecraft.getInstance().level;
        if (level == null || points.isEmpty() || duration <= 0) return;
        int last = points.size() - 1;
        for (int i = 0; i < last; i += 2) {
            var point = Vec3.atCenterOf(points.get(i));
            var next = Vec3.atCenterOf(points.get(Math.min(i + 2, last)));
            ParticleBuilder.create(EBParticles.PATH).pos(point).velocity(next.subtract(point).scale(1.0 / 45))
                    .time(duration).color(0, 1f, 0.3f).spawn(level);
        }
        ParticleBuilder.create(EBParticles.PATH).pos(Vec3.atCenterOf(points.getLast()))
                .time(duration).color(1f, 1f, 1f).spawn(level);
    }
}

