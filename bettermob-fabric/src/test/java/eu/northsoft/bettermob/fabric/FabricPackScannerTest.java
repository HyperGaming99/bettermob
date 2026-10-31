package eu.northsoft.bettermob.fabric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricPackScannerTest {

    @Test
    void scansFlatAndPackFolders(@TempDir Path tempDir) throws IOException {
        Path mobsFlat = tempDir.resolve("mobs");
        Files.createDirectories(mobsFlat);

        Path packA = tempDir.resolve("packs").resolve("pack_a").resolve("Mobs");
        Files.createDirectories(packA);

        Path packB = tempDir.resolve("packs").resolve("pack_b").resolve("mobs");
        Files.createDirectories(packB);

        Path packDisabled = tempDir.resolve("packs").resolve("pack_disabled").resolve("mobs");
        Files.createDirectories(packDisabled);

        Files.writeString(tempDir.resolve("config.yml"), """
                DisabledPacks:
                  - pack_disabled
                """);

        FabricPackScanner scanner = new FabricPackScanner(tempDir);
        List<File> folders = scanner.foldersFor("mobs");

        assertEquals(3, folders.size());
        assertTrue(folders.contains(mobsFlat.toFile()));
        assertTrue(folders.contains(packA.toFile()));
        assertTrue(folders.contains(packB.toFile()));

        List<String> packs = scanner.listPacks();
        assertEquals(3, packs.size());
        assertTrue(packs.contains("pack_a (enabled)"));
        assertTrue(packs.contains("pack_b (enabled)"));
        assertTrue(packs.contains("pack_disabled (disabled)"));
    }
}
