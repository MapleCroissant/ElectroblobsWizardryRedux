package com.binaris.wizardry.datagen;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.setup.registries.Spells;
import com.google.gson.JsonParser;
import net.minecraft.data.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;

/** Same canonical export and native spell-default regeneration as the Fabric port. */
@EventBusSubscriber(modid = WizardryMainMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgeDataGenerators {
    @SubscribeEvent public static void gather(GatherDataEvent event) {
        event.getGenerator().addProvider(true, new CanonicalResources(event.getGenerator().getPackOutput()));
    }
    private record CanonicalResources(PackOutput output) implements DataProvider {
        @Override public CompletableFuture<?> run(CachedOutput cache) {
            List<CompletableFuture<?>> writes = new ArrayList<>();
            var file = ModList.get().getModFileById(WizardryMainMod.MOD_ID).getFile();
            for (String folder : List.of("data", "assets")) {
                Path input = file.findResource(folder);
                try (var paths = Files.walk(input)) {
                    paths.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                        Path relative = input.relativize(path);
                        if (folder.equals("data") && relative.toString().replace('\\', '/').startsWith("ebwizardry/spells/")) return;
                        try (var reader = Files.newBufferedReader(path)) {
                            writes.add(DataProvider.saveStable(cache, JsonParser.parseReader(reader), output.getOutputFolder().resolve(folder).resolve(relative.toString())));
                        } catch (IOException error) { throw new java.io.UncheckedIOException(error); }
                    });
                } catch (IOException error) { return CompletableFuture.failedFuture(error); }
            }
            var target = output.createPathProvider(PackOutput.Target.DATA_PACK, "spells");
            Spells.SPELLS.values().stream().filter(spell -> !spell.property(DefaultProperties.SENSIBLE)).forEach(spell ->
                    writes.add(DataProvider.saveStable(cache, spell.getProperties().toJson(), target.json(spell.getLocation()))));
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        }
        @Override public String getName() { return "Wizardry canonical resources and spell defaults"; }
    }
}
