package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobDefinitionParser;
import eu.northsoft.bettermob.pack.YamlFiles;
import eu.northsoft.bettermob.platform.ConfigSection;
import eu.northsoft.bettermob.platform.YamlConfig;
import eu.northsoft.bettermob.util.Duplicates;
import eu.northsoft.bettermob.util.ReloadDiff;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages loading and caching of MobDefinitions from flat folders and packs.
 */
public final class FabricMobRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettermob");

    private final FabricPackScanner packScanner;
    private final Path mobsDir;
    private final Map<String, MobDefinition> mobs = new ConcurrentHashMap<>();
    private volatile Map<String, String> fingerprints = Map.of();

    public Map<String, String> fingerprints() {
        return fingerprints;
    }

    public FabricMobRegistry() {
        this(resolveDefaultDataDir());
    }

    private static Path resolveDefaultDataDir() {
        try {
            if (FabricLoader.getInstance() != null) {
                Path configDir = FabricLoader.getInstance().getConfigDir();
                if (configDir != null) {
                    return configDir.resolve("bettermob");
                }
            }
        } catch (Throwable ignored) {}
        return Path.of("config/bettermob");
    }

    public FabricMobRegistry(FabricPackScanner packScanner) {
        this.packScanner = packScanner;
        this.mobsDir = packScanner.dataFolder().resolve("mobs");
    }

    public FabricMobRegistry(Path path) {
        if (path.getFileName() != null && path.getFileName().toString().equalsIgnoreCase("mobs")) {
            this.mobsDir = path;
            this.packScanner = new FabricPackScanner(path.getParent() != null ? path.getParent() : path);
        } else {
            this.mobsDir = path.resolve("mobs");
            this.packScanner = new FabricPackScanner(path);
        }
    }

    public void load() {
        ensureDefaultTemplates();
        Map<String, MobDefinition> loaded = new LinkedHashMap<>();
        Map<String, String> prints = new LinkedHashMap<>();
        Duplicates duplicates = new Duplicates();

        for (File sourceFolder : packScanner.foldersFor("mobs")) {
            for (File file : YamlFiles.collect(sourceFolder)) {
                loadFile(file, sourceFolder.getPath(), loaded, prints, duplicates);
            }
        }

        duplicates.report((dup, folder, count) ->
                LOGGER.warn("Duplicate mob ID '{}' found in {} (count: {})", dup, folder, count));

        mobs.clear();
        mobs.putAll(loaded);
        fingerprints = prints;
        LOGGER.info("Loaded {} BetterMob definition(s)", mobs.size());
    }

    private void loadFile(File file, String sourceFolder, Map<String, MobDefinition> target, Map<String, String> prints, Duplicates duplicates) {
        String fileName = file.getName();
        int dot = fileName.lastIndexOf('.');
        String fileId = dot > 0 ? fileName.substring(0, dot) : fileName;

        try {
            ConfigSection root = YamlConfig.load(file);
            Map<String, ConfigSection> sections = MobDefinitionParser.extractSections(fileId, root);
            for (Map.Entry<String, ConfigSection> entry : sections.entrySet()) {
                String id = entry.getKey().toLowerCase(Locale.ROOT);
                if (target.containsKey(id)) {
                    duplicates.add(id, sourceFolder);
                    continue;
                }
                MobDefinition def = MobDefinitionParser.parse(id, entry.getValue(), LOGGER::warn);
                if (def != null) {
                    target.put(id, def);
                    prints.put(id, ReloadDiff.fingerprint(entry.getValue()));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse mob file {}", file, e);
        }
    }

    private void ensureDefaultTemplates() {
        if (!Files.exists(mobsDir)) {
            try {
                Files.createDirectories(mobsDir);
                saveDefaultTemplate("mobs/skeleton_knight.yml");
                saveDefaultTemplate("mobs/zombie_brute.yml");
                saveDefaultTemplate("mobs/nm_wumpus.yml");
            } catch (IOException e) {
                LOGGER.error("Failed to create default mobs directory at {}", mobsDir, e);
            }
        }
    }

    private void saveDefaultTemplate(String resourcePath) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) return;
            Path target = mobsDir.resolve(new File(resourcePath).getName());
            if (!Files.exists(target)) {
                Files.copy(in, target);
                LOGGER.info("Created default template: {}", target.getFileName());
            }
        } catch (IOException e) {
            LOGGER.warn("Could not copy default template {}", resourcePath, e);
        }
    }

    public MobDefinition get(String id) {
        if (id == null) return null;
        return mobs.get(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, MobDefinition> all() {
        return Collections.unmodifiableMap(mobs);
    }
}
