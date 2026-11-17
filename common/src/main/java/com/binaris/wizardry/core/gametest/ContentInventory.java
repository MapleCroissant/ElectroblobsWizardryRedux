package com.binaris.wizardry.core.gametest;

import com.binaris.wizardry.core.platform.Services;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import java.nio.file.*;
import java.util.*;

/** Runtime preservation evidence emitted by both loaders' migration regression. */
public final class ContentInventory {
    public static void write(ServerLevel level) {
        Map<String, List<String>> inventory = new TreeMap<>();
        BuiltInRegistries.REGISTRY.forEach(registry -> collect(inventory, registry));
        level.registryAccess().registries().forEach(entry -> collect(inventory, entry.value()));
        inventory.put("ebwizardry:spells", Services.REGISTRY_UTIL.getSpells().stream().map(spell -> spell.getLocation().toString()).sorted().toList());
        inventory.put("ebwizardry:elements", Services.REGISTRY_UTIL.getElements().stream().map(element -> element.getLocation().toString()).sorted().toList());
        inventory.put("ebwizardry:tiers", Services.REGISTRY_UTIL.getTiers().stream().map(tier -> tier.getOrCreateLocation().toString()).sorted().toList());
        try {
            Files.writeString(Path.of("ebwizardry-content-inventory.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(inventory));
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
    private static void collect(Map<String, List<String>> inventory, Registry<?> registry) {
        if (!registry.key().location().getNamespace().equals("minecraft")) return;
        var ids = registry.keySet().stream().filter(id -> id.getNamespace().equals("ebwizardry")).map(Object::toString).sorted().toList();
        if (!ids.isEmpty()) inventory.put(registry.key().location().toString(), ids);
    }
}
