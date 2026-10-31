package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.drop.DropEntry;
import eu.northsoft.bettermob.drop.DropRoller;
import eu.northsoft.bettermob.drop.DropTable;
import eu.northsoft.bettermob.pack.YamlFiles;
import eu.northsoft.bettermob.platform.ConfigSection;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.platform.YamlConfig;
import eu.northsoft.bettermob.util.Duplicates;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricDropRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettermob");

    private final FabricPackScanner packScanner;
    private final FabricItemRegistry itemRegistry;
    private final Map<String, DropTable> tables = new ConcurrentHashMap<>();
    private volatile Map<String, String> fingerprints = Map.of();

    public Map<String, String> fingerprints() {
        return fingerprints;
    }

    public FabricDropRegistry(FabricPackScanner packScanner, FabricItemRegistry itemRegistry) {
        this.packScanner = packScanner;
        this.itemRegistry = itemRegistry;
    }

    public void load() {
        Map<String, DropTable> loaded = new LinkedHashMap<>();
        Map<String, String> prints = new LinkedHashMap<>();
        Duplicates duplicates = new Duplicates();

        for (File sourceFolder : packScanner.foldersFor("droptables")) {
            for (File file : YamlFiles.collect(sourceFolder)) {
                try {
                    ConfigSection config = YamlConfig.load(file);
                    for (String id : config.getKeys(false)) {
                        ConfigSection section = config.getConfigurationSection(id);
                        if (section == null || !section.isList("Drops")) continue;

                        String lowerId = id.toLowerCase(Locale.ROOT);
                        if (loaded.containsKey(lowerId)) {
                            duplicates.add(id, sourceFolder.getPath());
                            continue;
                        }

                        List<DropEntry> entries = new ArrayList<>();
                        for (String line : section.getStringList("Drops")) {
                            DropEntry entry = DropEntry.parse(line);
                            if (entry == null) {
                                LOGGER.warn("Drop table '{}' line invalid: {}", id, line);
                            } else {
                                entries.add(entry);
                            }
                        }

                        int minItems = Math.max(0, section.getInt("MinItems", 0));
                        int maxItems = section.getInt("MaxItems", Integer.MAX_VALUE);
                        loaded.put(lowerId, new DropTable(id, minItems, maxItems, List.copyOf(entries)));
                        prints.put(lowerId, eu.northsoft.bettermob.util.ReloadDiff.fingerprint(section));
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load droptable file {}: {}", file, e.getMessage());
                }
            }
        }

        duplicates.report((dup, folder, count) ->
                LOGGER.warn("Duplicate drop table '{}' in {} (count: {})", dup, folder, count));

        tables.clear();
        tables.putAll(loaded);
        fingerprints = prints;
        LOGGER.info("Loaded {} BetterMob drop table(s)", tables.size());
    }

    public DropTable get(String id) {
        if (id == null) return null;
        return tables.get(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> ids() {
        return tables.keySet();
    }

    public Map<String, DropTable> all() {
        return Collections.unmodifiableMap(tables);
    }

    public DropRoller.RollResult roll(DropTable table) {
        return DropRoller.roll(table, this::get);
    }

    public void spawnDrop(ServerLevel level, Position pos, String itemKey, int amount) {
        if (itemKey == null || amount <= 0 || level == null || pos == null) return;

        FabricItemDefinition customItem = itemRegistry.get(itemKey);
        if (customItem != null) {
            ItemStack template = itemRegistry.create(customItem, 1);
            spawnStacks(level, pos, template, amount);
            return;
        }

        String normalized = itemKey.toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        Identifier id = Identifier.tryParse(normalized);
        if (id != null) {
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item != null && item != Items.AIR) {
                spawnStacks(level, pos, new ItemStack(item), amount);
                return;
            }
        }

        LOGGER.warn("Unresolvable drop item '{}'", itemKey);
    }

    private void spawnStacks(ServerLevel level, Position pos, ItemStack template, int amount) {
        int maxStack = Math.max(1, template.getMaxStackSize());
        while (amount > 0) {
            int count = Math.min(amount, maxStack);
            ItemStack stack = template.copyWithCount(count);
            ItemEntity entity = new ItemEntity(level, pos.x(), pos.y(), pos.z(), stack);
            level.addFreshEntity(entity);
            amount -= count;
        }
    }
}
