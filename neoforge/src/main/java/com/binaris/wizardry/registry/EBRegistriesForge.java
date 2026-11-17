package com.binaris.wizardry.registry;

import com.binaris.wizardry.api.content.spell.*;
import com.binaris.wizardry.core.registry.EBRegistries;
import net.minecraft.core.Registry;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public final class EBRegistriesForge {
    public static final Registry<Element> ELEMENT = new RegistryBuilder<>(EBRegistries.ELEMENT).sync(true).create();
    public static final Registry<SpellTier> TIER = new RegistryBuilder<>(EBRegistries.TIER).sync(true).create();
    public static final Registry<Spell> SPELL = new RegistryBuilder<>(EBRegistries.SPELL).sync(true).create();
    public static void register(NewRegistryEvent event) {
        event.register(ELEMENT); event.register(TIER); event.register(SPELL);
    }
}
