package com.binaris.wizardry.setup.registries;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.DeferredObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class EBAttributes {
    static Map<String, DeferredObject<Attribute>> ATTRIBUTES = new HashMap<>();

    public static final java.util.function.Supplier<net.minecraft.core.Holder<Attribute>> CAST_POTENCY = attribute("cast.potency");
    public static final java.util.function.Supplier<net.minecraft.core.Holder<Attribute>> CAST_COST = attribute("cast.cost");
    public static final java.util.function.Supplier<net.minecraft.core.Holder<Attribute>> CAST_CHARGEUP = attribute("cast.chargeup");
    public static final java.util.function.Supplier<net.minecraft.core.Holder<Attribute>> CAST_PROGRESSION = attribute("cast.progression");
    public static final java.util.function.Supplier<net.minecraft.core.Holder<Attribute>> CAST_DURATION = attribute("cast.duration");
    public static final java.util.function.Supplier<net.minecraft.core.Holder<Attribute>> CAST_BLAST = attribute("cast.blast");
    public static final java.util.function.Supplier<net.minecraft.core.Holder<Attribute>> CAST_RANGE = attribute("cast.range");
    public static final java.util.function.Supplier<net.minecraft.core.Holder<Attribute>> CAST_COOLDOWN = attribute("cast.cooldown");

    public static void register(RegisterFunction<Attribute> function) {
        ATTRIBUTES.forEach(((id, attribute) ->
                function.register(BuiltInRegistries.ATTRIBUTE, WizardryMainMod.location(id), attribute.get())));
    }

    public static Collection<java.util.function.Supplier<net.minecraft.core.Holder<Attribute>>> getAttributes() {
        return ATTRIBUTES.values().stream().<java.util.function.Supplier<net.minecraft.core.Holder<Attribute>>>map(value -> () -> BuiltInRegistries.ATTRIBUTE.wrapAsHolder(value.get())).toList();
    }

    static java.util.function.Supplier<net.minecraft.core.Holder<Attribute>> attribute(String name) {
        return attribute(name, 0, -100, 100, true);
    }

    static java.util.function.Supplier<net.minecraft.core.Holder<Attribute>> attribute(String name, float defaultValue, float minValue, float maxValue, boolean sync) {
        Attribute attribute = new RangedAttribute("attribute.name." + name, defaultValue, minValue, maxValue).setSyncable(sync);
        DeferredObject<Attribute> ret = new DeferredObject<>(() -> attribute);
        ATTRIBUTES.put(name, ret);
        return () -> BuiltInRegistries.ATTRIBUTE.wrapAsHolder(ret.get());
    }

    private EBAttributes() {
    }
}