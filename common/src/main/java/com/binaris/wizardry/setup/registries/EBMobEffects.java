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

    public static final Supplier<net.minecraft.core.Holder<MobEffect>> ARCANE_JAMMER = mobEffect("arcane_jammer", ArcaneJammerMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> FROST = mobEffect("frost", FrostMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> PARALYSIS = mobEffect("paralysis", ParalysisMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> STATIC_AURA = mobEffect("static_aura", StaticAuraMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> WARD = mobEffect("ward", WardMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> FIRESKIN = mobEffect("fireskin", FireSkinMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> OAKFLESH = mobEffect("oakflesh", OakFleshMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> CURSE_OF_ENFEEBLEMENT = mobEffect("curse_of_enfeeblement", EnfeeblementCurse::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> CURSE_OF_UNDEATH = mobEffect("curse_of_undeath", UndeathCurse::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> DECAY = mobEffect("decay", DecayMobEffect::new);
    public static final Supplier<net.minecraft.core.Holder<MobEffect>> CONTAINMENT = mobEffect("containment", ContainmentEffect::new);
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
