package com.binaris.wizardry.datagen;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.content.spell.DefaultProperties;
import com.binaris.wizardry.setup.registries.Spells;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Fabric-only regeneration of spell defaults and export of the canonical migrated data. */
public final class EBFabricDataGenerators implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        generator.createPack().addProvider(CanonicalResources::new);
    }

    private static final class CanonicalResources implements DataProvider {
        private final FabricDataOutput output;

        private CanonicalResources(FabricDataOutput output) {
            this.output = output;
        }

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            List<CompletableFuture<?>> writes = new ArrayList<>();
            var mod = FabricLoader.getInstance().getModContainer(WizardryMainMod.MOD_ID).orElseThrow();
            for (Path root : mod.getRootPaths()) {
                for (String folder : List.of("data", "assets")) {
                    Path input = root.resolve(folder);
                    if (!Files.isDirectory(input)) continue;
                    try (var paths = Files.walk(input)) {
                        paths.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                            Path relative = root.relativize(path);
                            // Spell defaults are regenerated from the registered Java definitions below.
                            if (relative.toString().replace('\\', '/').startsWith("data/ebwizardry/spells/")) return;
                            try (var reader = Files.newBufferedReader(path)) {
                                writes.add(DataProvider.saveStable(cache, JsonParser.parseReader(reader), output.getOutputFolder().resolve(relative.toString())));
                            } catch (IOException error) {
                                throw new java.io.UncheckedIOException(error);
                            }
                        });
                    } catch (IOException error) {
                        return CompletableFuture.failedFuture(error);
                    }
                }
            }
            var spells = output.createPathProvider(net.minecraft.data.PackOutput.Target.DATA_PACK, "spells");
            Spells.SPELLS.values().stream().filter(spell -> !spell.property(DefaultProperties.SENSIBLE)).forEach(spell ->
                    writes.add(DataProvider.saveStable(cache, spell.getProperties().toJson(), spells.json(spell.getLocation()))));
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        }

        @Override
        public String getName() {
            return "Wizardry canonical resource export and spell defaults";
        }
    }
}
