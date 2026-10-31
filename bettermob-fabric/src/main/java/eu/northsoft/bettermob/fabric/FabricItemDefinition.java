package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.mob.MobDefinition;
import net.minecraft.item.Item;

import java.util.List;

public record FabricItemDefinition(
        String id,
        Item item,
        int model,
        String displayName,
        List<String> lore,
        List<MobDefinition.SkillTrigger> triggers
) {}
