package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.drop.DropRoller;
import eu.northsoft.bettermob.drop.DropTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricDropRegistryTest {
    @Test
    void loadsDropTablesAndResolvesNestedDrops(@TempDir Path tempDir) throws IOException {
        Path dropsDir = tempDir.resolve("droptables");
        Files.createDirectories(dropsDir);

        String yaml = """
                nested_gems:
                  MinItems: 1
                  MaxItems: 1
                  Drops:
                    - EMERALD 1 100%
                    - EXP 5 100%

                boss_drops:
                  MinItems: 2
                  MaxItems: 2
                  Drops:
                    - DIAMOND 2 100%
                    - nested_gems 1 100%
                    - EXP 10 100%
                """;

        Files.writeString(dropsDir.resolve("drops.yml"), yaml);

        FabricPackScanner scanner = new FabricPackScanner(tempDir);
        FabricItemRegistry itemRegistry = new FabricItemRegistry(scanner);
        FabricDropRegistry dropRegistry = new FabricDropRegistry(scanner, itemRegistry);

        dropRegistry.load();

        DropTable bossDrops = dropRegistry.get("boss_drops");
        assertNotNull(bossDrops);
        assertEquals(2, bossDrops.minItems());
        assertEquals(2, bossDrops.maxItems());

        DropRoller.RollResult rolled = dropRegistry.roll(bossDrops);
        assertNotNull(rolled);

        assertEquals(15, rolled.exp());

        assertEquals(2, rolled.items().size());
        assertTrue(rolled.items().stream().anyMatch(d -> d.itemKey().equalsIgnoreCase("DIAMOND") && d.amount() == 2));
        assertTrue(rolled.items().stream().anyMatch(d -> d.itemKey().equalsIgnoreCase("EMERALD") && d.amount() == 1));
    }
}
