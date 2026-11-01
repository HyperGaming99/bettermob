package eu.northsoft.bettermob.mob;

import java.util.Locale;

public final class DamageCauses {
    private DamageCauses() {}

    public static String causeOf(String damageTypeId) {
        return causeOf(damageTypeId, false, false, false, false, false);
    }

    public static String causeOf(String damageTypeId, boolean explosion, boolean directEntity, boolean tnt, boolean projectile, boolean potion) {
        if (explosion) {
            if (!directEntity || tnt) {
                return "BLOCK_EXPLOSION";
            }
            return "ENTITY_EXPLOSION";
        }

        String key = normalize(damageTypeId);

        if (directEntity) {
            if (projectile) {
                if (potion) {
                    return "MAGIC";
                }
                return "PROJECTILE";
            }
            return switch (key) {
                case "thorns" -> "THORNS";
                case "sonic_boom" -> "SONIC_BOOM";
                case "falling_stalactite", "falling_block", "falling_anvil" -> "FALLING_BLOCK";
                case "lightning_bolt" -> "LIGHTNING";
                case "fall" -> "FALL";
                case "dragon_breath" -> "DRAGON_BREATH";
                case "magic" -> "MAGIC";
                default -> "ENTITY_ATTACK";
            };
        }

        return switch (key) {
            case "out_of_world", "fell_out_of_world" -> "VOID";
            case "lava" -> "LAVA";
            case "cactus", "sweet_berry_bush", "stalagmite" -> "CONTACT";
            case "hot_floor" -> "HOT_FLOOR";
            case "in_fire" -> "FIRE";
            case "campfire" -> "CAMPFIRE";
            case "starve" -> "STARVATION";
            case "wither" -> "WITHER";
            case "in_wall" -> "SUFFOCATION";
            case "drown" -> "DROWNING";
            case "on_fire" -> "FIRE_TICK";
            case "magic", "indirect_magic" -> "MAGIC";
            case "fall", "ender_pearl" -> "FALL";
            case "fly_into_wall" -> "FLY_INTO_WALL";
            case "cramming" -> "CRAMMING";
            case "dry_out" -> "DRYOUT";
            case "freeze" -> "FREEZE";
            case "generic_kill" -> "KILL";
            case "outside_border" -> "WORLD_BORDER";
            default -> "CUSTOM";
        };
    }

    private static String normalize(String damageTypeId) {
        if (damageTypeId == null || damageTypeId.isBlank()) {
            return "";
        }
        String key = damageTypeId.toLowerCase(Locale.ROOT).trim();
        if (key.startsWith("minecraft:")) {
            key = key.substring("minecraft:".length());
        }
        return key;
    }
}
