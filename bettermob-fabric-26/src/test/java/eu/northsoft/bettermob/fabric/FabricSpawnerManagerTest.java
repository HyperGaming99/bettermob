package eu.northsoft.bettermob.fabric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricSpawnerManagerTest {

    @Test
    void loadsSpawnersFromYamlFile(@TempDir Path tempDir) throws IOException {
        Path spawnersYml = tempDir.resolve("spawners.yml");
        String yaml = """
                graveyard_knight:
                  mob: skeleton_knight
                  world: minecraft:overworld
                  x: 100.5
                  y: 65.0
                  z: -200.5
                  radius: 8
                  interval: 15
                  max: 4
                  player-range: 40
                """;
        Files.writeString(spawnersYml, yaml);

        BetterMobFabricMod mod = new BetterMobFabricMod();
        FabricSpawnerManager manager = new FabricSpawnerManager(mod, spawnersYml);
        manager.load();

        assertEquals(1, manager.spawners().size());
        FabricSpawnerManager.ActiveSpawner spawner = manager.spawners().get("graveyard_knight");
        assertNotNull(spawner);
        assertEquals("skeleton_knight", spawner.definition().mob());
        assertEquals("minecraft:overworld", spawner.definition().world());
        assertEquals(100.5, spawner.definition().x());
        assertEquals(65.0, spawner.definition().y());
        assertEquals(-200.5, spawner.definition().z());
        assertEquals(8, spawner.definition().radius());
        assertEquals(15, spawner.definition().intervalSeconds());
        assertEquals(4, spawner.definition().max());
        assertEquals(40, spawner.definition().playerRange());
    }

    @Test
    void buildsAndReadsSpawnerTags() {
        assertEquals("bettermob.spawner.graveyard_knight", FabricSpawnerManager.tagOf("Graveyard_Knight"));
        assertEquals("graveyard_knight", FabricSpawnerManager.spawnerIdOf(List.of("other", "bettermob.spawner.graveyard_knight")));
        assertNull(FabricSpawnerManager.spawnerIdOf(List.of("other", "bettermob.spawner.")));
        assertNull(FabricSpawnerManager.spawnerIdOf(null));
    }

    @Test
    void keepsAliveMobsAcrossReload(@TempDir Path tempDir) throws IOException {
        Path spawnersYml = writeSpawner(tempDir);
        FabricSpawnerManager manager = new FabricSpawnerManager(new BetterMobFabricMod(), spawnersYml);
        manager.load();
        UUID mob = UUID.randomUUID();
        manager.track(mob, Set.of(FabricSpawnerManager.tagOf("graveyard_knight")));
        manager.spawners().get("graveyard_knight").nextSpawnTick(1200);
        manager.spawners().get("graveyard_knight").reconciled(true);

        manager.load();

        FabricSpawnerManager.ActiveSpawner spawner = manager.spawners().get("graveyard_knight");
        assertTrue(spawner.alive().contains(mob));
        assertEquals(1200, spawner.nextSpawnTick());
        assertTrue(spawner.reconciled());
    }

    @Test
    void newSpawnerStartsUnreconciled(@TempDir Path tempDir) throws IOException {
        FabricSpawnerManager manager = new FabricSpawnerManager(new BetterMobFabricMod(), writeSpawner(tempDir));
        manager.load();
        assertFalse(manager.spawners().get("graveyard_knight").reconciled());
    }

    @Test
    void tracksOnlyKnownSpawnersAndUntracksOnRemoval(@TempDir Path tempDir) throws IOException {
        FabricSpawnerManager manager = new FabricSpawnerManager(new BetterMobFabricMod(), writeSpawner(tempDir));
        manager.load();
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();

        manager.track(known, Set.of(FabricSpawnerManager.tagOf("graveyard_knight")));
        manager.track(unknown, Set.of(FabricSpawnerManager.tagOf("missing")));
        manager.track(UUID.randomUUID(), Set.of("unrelated"));

        Set<UUID> alive = manager.spawners().get("graveyard_knight").alive();
        assertEquals(Set.of(known), alive);

        manager.untrack(known);
        assertFalse(alive.contains(known));
    }

    private static Path writeSpawner(Path tempDir) throws IOException {
        Path spawnersYml = tempDir.resolve("spawners.yml");
        Files.writeString(spawnersYml, """
                graveyard_knight:
                  mob: skeleton_knight
                  world: minecraft:overworld
                  x: 0
                  y: 64
                  z: 0
                  max: 1
                """);
        return spawnersYml;
    }
}
