package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.mob.ExtraTriggers;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.pack.YamlFiles;
import eu.northsoft.bettermob.platform.ConfigSection;
import eu.northsoft.bettermob.platform.YamlConfig;
import eu.northsoft.bettermob.util.Duplicates;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
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

public final class FabricItemRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettermob");

    private final FabricPackScanner packScanner;
    private final Map<String, FabricItemDefinition> items = new ConcurrentHashMap<>();
    private volatile Map<String, String> fingerprints = Map.of();

    public Map<String, String> fingerprints() {
        return fingerprints;
    }

    public FabricItemRegistry(FabricPackScanner packScanner) {
        this.packScanner = packScanner;
    }

    public void load() {
        Map<String, FabricItemDefinition> loaded = new LinkedHashMap<>();
        Map<String, String> prints = new LinkedHashMap<>();
        Duplicates duplicates = new Duplicates();

        for (File sourceFolder : packScanner.foldersFor("items")) {
            for (File file : YamlFiles.collect(sourceFolder)) {
                try {
                    ConfigSection config = YamlConfig.load(file);
                    for (String id : config.getKeys(false)) {
                        ConfigSection section = config.getConfigurationSection(id);
                        if (section == null || !section.contains("Id")) continue;

                        String lowerId = id.toLowerCase(Locale.ROOT);
                        if (loaded.containsKey(lowerId)) {
                            duplicates.add(id, sourceFolder.getPath());
                            continue;
                        }

                        FabricItemDefinition def = parse(id, section);
                        if (def != null) {
                            loaded.put(lowerId, def);
                            prints.put(lowerId, eu.northsoft.bettermob.util.ReloadDiff.fingerprint(section));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load item file {}: {}", file, e.getMessage());
                }
            }
        }

        duplicates.report((dup, folder, count) ->
                LOGGER.warn("Duplicate item '{}' in {} (count: {})", dup, folder, count));

        items.clear();
        items.putAll(loaded);
        fingerprints = prints;
        LOGGER.info("Loaded {} BetterMob item definition(s)", items.size());
    }

    public FabricItemDefinition get(String id) {
        if (id == null) return null;
        return items.get(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> ids() {
        return items.keySet();
    }

    public Map<String, FabricItemDefinition> all() {
        return Collections.unmodifiableMap(items);
    }

    public FabricItemDefinition definitionOf(ItemStack stack) {
        String id = idOf(stack);
        return id == null ? null : get(id);
    }

    public String idOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        return customData.copyTag().getStringOr("bettermob:item_id", null);
    }

    public ItemStack create(FabricItemDefinition def, int amount) {
        ItemStack stack = new ItemStack(def.item(), Math.max(1, amount));

        if (def.displayName() != null && !def.displayName().isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(def.displayName().replace('&', '§')));
        }

        if (def.lore() != null && !def.lore().isEmpty()) {
            List<Component> lines = def.lore().stream()
                    .map(l -> (Component) Component.literal(l.replace('&', '§')))
                    .toList();
            stack.set(DataComponents.LORE, new ItemLore(lines));
        }

        if (def.model() >= 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(List.of((float) def.model()), List.of(), List.of(), List.of()));
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("bettermob:item_id", def.id()));
        return stack;
    }

    private FabricItemDefinition parse(String id, ConfigSection section) {
        String rawMaterial = section.getString("Id", "");
        if (rawMaterial == null || rawMaterial.isBlank()) {
            LOGGER.warn("Item '{}' has empty Id", id);
            return null;
        }

        String normalized = rawMaterial.toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }

        Identifier itemId = Identifier.tryParse(normalized);
        if (itemId == null) {
            LOGGER.warn("Item '{}' has invalid identifier: {}", id, rawMaterial);
            return null;
        }

        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null || item == Items.AIR) {
            LOGGER.warn("Item '{}' specifies unknown material '{}'", id, rawMaterial);
            return null;
        }

        List<MobDefinition.SkillTrigger> triggers = new ArrayList<>();
        for (String line : section.getStringList("Skills")) {
            MobDefinition.SkillTrigger trigger = MobDefinition.SkillTrigger.parse(line);
            if (trigger == null) {
                ExtraTriggers.warnUnparsed(line, () -> LOGGER.warn("Item '{}' skill line invalid: {}", id, line));
            } else {
                triggers.add(trigger);
            }
        }

        int model = section.getInt("Model", -1);
        String display = section.getString("Display", id);
        List<String> lore = List.copyOf(section.getStringList("Lore"));

        return new FabricItemDefinition(id, item, model, display, lore, List.copyOf(triggers));
    }
}
