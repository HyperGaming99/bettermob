package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.ConfigSection;
import eu.northsoft.bettermob.platform.YamlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FabricPackScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettermob");

    private final Path dataFolder;
    private final File packsFolder;

    public FabricPackScanner(Path dataFolder) {
        this.dataFolder = dataFolder;
        this.packsFolder = dataFolder.resolve("packs").toFile();
    }

    public Path dataFolder() {
        return dataFolder;
    }

    public File packsFolder() {
        return packsFolder;
    }

    public List<File> foldersFor(String name) {
        List<File> folders = new ArrayList<>();
        File flat = dataFolder.resolve(name).toFile();
        if (flat.isDirectory()) {
            folders.add(flat);
        }

        if (!packsFolder.isDirectory()) {
            packsFolder.mkdirs();
            return folders;
        }

        List<String> disabled = loadDisabledPacks();

        File[] packDirs = packsFolder.listFiles(File::isDirectory);
        if (packDirs == null) {
            return folders;
        }
        Arrays.sort(packDirs);
        for (File packDir : packDirs) {
            if (disabled.contains(packDir.getName().toLowerCase(Locale.ROOT))) {
                continue;
            }
            File subfolder = findCaseInsensitive(packDir, name);
            if (subfolder != null && subfolder.isDirectory()) {
                folders.add(subfolder);
            }
        }
        return folders;
    }

    public List<String> listPacks() {
        List<String> result = new ArrayList<>();
        if (!packsFolder.isDirectory()) {
            return result;
        }
        File[] packDirs = packsFolder.listFiles(File::isDirectory);
        if (packDirs == null) {
            return result;
        }
        Arrays.sort(packDirs);
        List<String> disabled = loadDisabledPacks();
        for (File packDir : packDirs) {
            boolean enabled = !disabled.contains(packDir.getName().toLowerCase(Locale.ROOT));
            result.add(packDir.getName() + " (" + (enabled ? "enabled" : "disabled") + ")");
        }
        return result;
    }

    public Map<String, File> sources() {
        Map<String, File> sources = new LinkedHashMap<>();
        sources.put("", dataFolder.toFile());
        if (!packsFolder.isDirectory()) {
            return sources;
        }
        File[] packDirs = packsFolder.listFiles(File::isDirectory);
        if (packDirs == null) {
            return sources;
        }
        Arrays.sort(packDirs);
        for (File packDir : packDirs) {
            sources.put(packDir.getName(), packDir);
        }
        return sources;
    }

    private List<String> loadDisabledPacks() {
        File configFile = dataFolder.resolve("config.yml").toFile();
        if (!configFile.exists()) {
            return Collections.emptyList();
        }
        try {
            ConfigSection config = YamlConfig.load(configFile);
            List<String> list = config.getStringList("DisabledPacks");
            if (list == null) return Collections.emptyList();
            return list.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
        } catch (Exception e) {
            LOGGER.warn("Failed to read DisabledPacks from {}: {}", configFile, e.getMessage());
            return Collections.emptyList();
        }
    }

    private static File findCaseInsensitive(File parent, String name) {
        File[] children = parent.listFiles(File::isDirectory);
        if (children == null) return null;
        for (File child : children) {
            if (child.getName().equalsIgnoreCase(name)) {
                return child;
            }
        }
        return null;
    }
}
