package com.binaris.wizardry.migration;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;

import java.nio.file.Files;
import java.nio.file.Path;

/** Offline conversion into a separate review directory; never overwrites source templates. */
public final class StructureMigrationTool {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Files.createDirectories(output);
        int failures = 0;
        try (var files = Files.list(input)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".nbt")).sorted().toList()) {
                CompoundTag original = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                int version = original.getInt("DataVersion");
                try {
                    CompoundTag converted = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), original.copy(), version);
                    if (original.getList("blocks", 10).size() != converted.getList("blocks", 10).size()
                            || original.getList("entities", 10).size() != converted.getList("entities", 10).size()) {
                        throw new IllegalStateException("Conversion changed block or entity counts");
                    }
                    converted.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                    NbtIo.writeCompressed(converted, output.resolve(file.getFileName()));
                    System.out.println(file.getFileName() + ": " + version + " -> " + converted.getInt("DataVersion")
                            + ", blocks=" + converted.getList("blocks", 10).size()
                            + ", entities=" + converted.getList("entities", 10).size());
                    if (!converted.getList("entities", 10).isEmpty()) {
                        System.out.println("  Entity data: " + converted.getList("entities", 10));
                    }
                } catch (RuntimeException error) {
                    failures++;
                    System.err.println(file.getFileName() + ": version=" + version + ", failed: " + error);
                    error.printStackTrace();
                }
            }
        }
        if (failures != 0) throw new IllegalStateException(failures + " structure conversions failed");
    }
}
