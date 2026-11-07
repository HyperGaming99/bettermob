package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.platform.ConfigSection;
import eu.northsoft.bettermob.platform.PlatformWorld;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.platform.YamlConfig;
import eu.northsoft.bettermob.spawner.SpawnerDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class FabricSpawnerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettermob");
    private static final String TAG_PREFIX = "bettermob.spawner.";

    public static final class ActiveSpawner {
        private final SpawnerDefinition definition;
        private final Set<UUID> alive = ConcurrentHashMap.newKeySet();
        private long nextSpawnTick = 0;
        private volatile boolean reconciled;

        public ActiveSpawner(SpawnerDefinition definition) {
            this.definition = definition;
        }

        public SpawnerDefinition definition() {
            return definition;
        }

        public Set<UUID> alive() {
            return alive;
        }

        public long nextSpawnTick() {
            return nextSpawnTick;
        }

        public void nextSpawnTick(long tick) {
            this.nextSpawnTick = tick;
        }

        public boolean reconciled() {
            return reconciled;
        }

        public void reconciled(boolean reconciled) {
            this.reconciled = reconciled;
        }
    }

    private final BetterMobFabricMod mod;
    private final Path configFile;
    private final Map<String, ActiveSpawner> spawners = new ConcurrentHashMap<>();
    private volatile Map<String, String> fingerprints = Map.of();
    private long currentTick = 0;

    public FabricSpawnerManager(BetterMobFabricMod mod, Path configFile) {
        this.mod = mod;
        this.configFile = configFile;
    }

    public Map<String, ActiveSpawner> spawners() {
        return spawners;
    }

    public Map<String, String> fingerprints() {
        return fingerprints;
    }

    public static String tagOf(String spawnerId) {
        return TAG_PREFIX + spawnerId.toLowerCase(java.util.Locale.ROOT);
    }

    public static String spawnerIdOf(Collection<String> tags) {
        if (tags == null) return null;
        for (String tag : tags) {
            if (tag != null && tag.startsWith(TAG_PREFIX) && tag.length() > TAG_PREFIX.length()) {
                return tag.substring(TAG_PREFIX.length());
            }
        }
        return null;
    }

    public void track(UUID uuid, Collection<String> tags) {
        String id = spawnerIdOf(tags);
        if (uuid == null || id == null) return;
        ActiveSpawner spawner = spawners.get(id);
        if (spawner != null) spawner.alive().add(uuid);
    }

    public void untrack(UUID uuid) {
        if (uuid == null) return;
        for (ActiveSpawner spawner : spawners.values()) {
            spawner.alive().remove(uuid);
        }
    }

    public void load() {
        Map<String, ActiveSpawner> previous = new java.util.HashMap<>(spawners);
        spawners.clear();
        Map<String, String> prints = new java.util.LinkedHashMap<>();
        File file = configFile.toFile();
        if (!file.exists()) {
            this.fingerprints = prints;
            return;
        }
        try {
            ConfigSection root = YamlConfig.load(file);
            for (String id : root.getKeys(false)) {
                ConfigSection sec = root.getConfigurationSection(id);
                SpawnerDefinition def = SpawnerDefinition.read(id, sec);
                if (def != null) {
                    String lower = id.toLowerCase(java.util.Locale.ROOT);
                    ActiveSpawner spawner = new ActiveSpawner(def);
                    ActiveSpawner old = previous.get(lower);
                    if (old != null) {
                        spawner.alive().addAll(old.alive());
                        spawner.nextSpawnTick(old.nextSpawnTick());
                        spawner.reconciled(old.reconciled());
                    }
                    spawners.put(lower, spawner);
                    prints.put(lower, eu.northsoft.bettermob.util.ReloadDiff.fingerprint(sec));
                }
            }
            this.fingerprints = prints;
            LOGGER.info("Loaded {} spawner(s) from {}", spawners.size(), file.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to load spawners from {}: {}", file.getPath(), e.getMessage());
        }
    }

    public void tick() {
        currentTick++;
        if (currentTick % 20 != 0 || spawners.isEmpty()) return;

        for (ActiveSpawner spawner : spawners.values()) {
            SpawnerDefinition def = spawner.definition();
            PlatformWorld world = mod.platform().world(def.world()).orElse(null);
            if (world == null) continue;

            if (!spawner.reconciled()) {
                if (!FabricAdapters.entitiesLoaded(world, def.x(), def.z(), def.radius())) continue;
                spawner.reconciled(true);
            }

            if (spawner.alive().size() >= def.max()) continue;
            if (currentTick < spawner.nextSpawnTick()) continue;

            Position center = new Position(def.world(), def.x(), def.y(), def.z(), 0, 0);

            var player = world.nearestPlayer(center, def.playerRange());
            if (player.isEmpty()) continue;

            double offsetX = def.radius() > 0 ? (ThreadLocalRandom.current().nextDouble() * 2 - 1) * def.radius() : 0;
            double offsetZ = def.radius() > 0 ? (ThreadLocalRandom.current().nextDouble() * 2 - 1) * def.radius() : 0;
            Position spawnPos = new Position(def.world(), def.x() + offsetX, def.y(), def.z() + offsetZ,
                    ThreadLocalRandom.current().nextFloat() * 360f, 0);

            MobDefinition mobDef = mod.mobRegistry().get(def.mob());
            if (mobDef == null) continue;

            var spawned = world.spawn(mobDef.type, spawnPos);
            if (spawned.isPresent() && spawned.get() instanceof eu.northsoft.bettermob.platform.PlatformLivingEntity living) {
                if (mobDef.displayName != null && !mobDef.displayName.isBlank()) {
                    living.customName(mobDef.displayName.replace('&', '§'));
                    living.customNameVisible(true);
                }
                if (mobDef.health > 0) {
                    living.maxHealth(mobDef.health);
                    living.health(mobDef.health);
                }
                if (mod.platform().model("bettermodel").isPresent() && mobDef.modelId != null) {
                    mod.platform().model("bettermodel").get().attach(living, mobDef.modelId);
                }
                FabricAdapters.addTag(living, tagOf(def.id()));
                mod.mobManager().register(living.uuid(), mobDef, living);
                spawner.alive().add(living.uuid());
                spawner.nextSpawnTick(currentTick + ((long) def.intervalSeconds() * 20));
            }
        }
    }
}
