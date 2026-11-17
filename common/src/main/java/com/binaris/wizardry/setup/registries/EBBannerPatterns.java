package com.binaris.wizardry.setup.registries;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.DeferredObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BannerPattern;

import java.util.HashMap;
import java.util.Map;

public final class EBBannerPatterns {
    static final Map<String, DeferredObject<BannerPattern>> PATTERNS_REGISTER = new HashMap<>();

    public static final DeferredObject<BannerPattern> EARTH = pattern("earth");
    public static final DeferredObject<BannerPattern> FIRE = pattern("fire");
    public static final DeferredObject<BannerPattern> HEALING = pattern("healing");
    public static final DeferredObject<BannerPattern> ICE = pattern("ice");
    public static final DeferredObject<BannerPattern> LIGHTNING = pattern("lightning");
    public static final DeferredObject<BannerPattern> NECROMANCY = pattern("necromancy");
    public static final DeferredObject<BannerPattern> SORCERY = pattern("sorcery");

    private EBBannerPatterns() {
    }

    static DeferredObject<BannerPattern> pattern(String name) {
        var ret = new DeferredObject<>(() -> new BannerPattern(WizardryMainMod.location(name), "block.minecraft.banner.ebwizardry." + name));
        PATTERNS_REGISTER.put(name, ret);
        return ret;
    }

    /**
     * Unique short code stored in banner NBT, mirrors the original mod's "eb" + initial scheme.
     */
    private static String bannerHash(String name) {
        return "eb" + name.charAt(0);
    }

    // ======= Registry =======
    public static void bootstrap(net.minecraft.data.worldgen.BootstrapContext<BannerPattern> context) {
        PATTERNS_REGISTER.forEach((id, pattern) ->
                context.register(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BANNER_PATTERN, WizardryMainMod.location(id)), pattern.get()));
    }
}
