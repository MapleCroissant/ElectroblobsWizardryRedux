package com.binaris.wizardry.setup.registries;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.DeferredObject;
import com.binaris.wizardry.api.content.effect.CurseMobEffect;
import com.binaris.wizardry.content.effect.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class EBMobEffects {
    static Map<String, DeferredObject<MobEffect>> MOB_EFFECTS = new HashMap<>();
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> FEAR = mobEffect("fear", FearMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> MUFFLE = mobEffect("muffle",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x4464d9) {});
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> MIRAGE = mobEffect("mirage",
            () -> new com.binaris.wizardry.api.content.effect.MagicMobEffect(MobEffectCategory.BENEFICIAL, 0xa378e6) {
                @Override
                public void spawnCustomParticle(net.minecraft.world.level.Level level, double x, double y, double z) {}
            });
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> SLOW_TIME = mobEffect("slow_time", SlowTimeMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> MIND_CONTROL = mobEffect("mind_control", MindControlMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> MIND_TRICK = mobEffect("mind_trick",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xcc33ff) {});

    public static final Supplier<net.minecraft.core.Holder<MobEffect>> ICE_SHROUD = mobEffect("ice_shroud", IceShroudMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> MARK_OF_SACRIFICE = mobEffect("mark_of_sacrifice",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xe90e48) {});
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> ARCANE_JAMMER = mobEffect("arcane_jammer", ArcaneJammerMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> FROST = mobEffect("frost", FrostMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> PARALYSIS = mobEffect("paralysis", ParalysisMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> STATIC_AURA = mobEffect("static_aura", StaticAuraMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> TRANSIENCE = mobEffect("transience", TransienceMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> SIXTH_SENSE = mobEffect("sixth_sense",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xc6ff01) {});
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> WARD = mobEffect("ward", WardMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> FIRESKIN = mobEffect("fireskin", FireSkinMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> IRONFLESH = mobEffect("ironflesh", () -> new MineralFleshMobEffect(false));
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> DIAMONDFLESH = mobEffect("diamondflesh", () -> new MineralFleshMobEffect(true));
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> OAKFLESH = mobEffect("oakflesh", OakFleshMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> CURSE_OF_ENFEEBLEMENT = mobEffect("curse_of_enfeeblement", EnfeeblementCurse::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> CURSE_OF_UNDEATH = mobEffect("curse_of_undeath", UndeathCurse::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> DECAY = mobEffect("decay", DecayMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> CONTAINMENT = mobEffect("containment", ContainmentEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> EMPOWERMENT = mobEffect("empowerment", EmpowermentMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> FONT_OF_MANA = mobEffect("font_of_mana", FontOfManaMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> CURSE_OF_SOULBINDING = mobEffect("curse_of_soulbinding", () -> new CurseMobEffect(MobEffectCategory.HARMFUL, 0x0f000f));
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> FROST_STEP = mobEffect("frost_step", FrostStepEffect::new);

    // ======= Registry =======
    public static void register(RegisterFunction<MobEffect> function) {
        MOB_EFFECTS.forEach(((id, mobEffect) ->
                function.register(BuiltInRegistries.MOB_EFFECT, WizardryMainMod.location(id), mobEffect.get())));
    }

    // ======= Helpers =======
    private static Supplier<net.minecraft.core.Holder<MobEffect>> mobEffect(String name, Supplier<MobEffect> effect) {
        DeferredObject<MobEffect> ret = new DeferredObject<>(effect);
        MOB_EFFECTS.put(name, ret);
        return () -> BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ret.get());
    }

}
