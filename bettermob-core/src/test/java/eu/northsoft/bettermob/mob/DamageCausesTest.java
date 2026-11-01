package eu.northsoft.bettermob.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageCausesTest {

    @Test
    void resolvesExplosionContext() {
        assertEquals("BLOCK_EXPLOSION", DamageCauses.causeOf("explosion", true, true, true, false, false));
        assertEquals("BLOCK_EXPLOSION", DamageCauses.causeOf("explosion", true, false, false, false, false));
        assertEquals("ENTITY_EXPLOSION", DamageCauses.causeOf("fireworks", true, true, false, false, false));
        assertEquals("ENTITY_EXPLOSION", DamageCauses.causeOf("wind_charge", true, true, false, false, false));
        assertEquals("ENTITY_EXPLOSION", DamageCauses.causeOf("explosion", true, true, false, false, false));
        assertEquals("ENTITY_EXPLOSION", DamageCauses.causeOf("player_explosion", true, true, false, false, false));
    }

    @Test
    void resolvesDirectEntityContext() {
        assertEquals("PROJECTILE", DamageCauses.causeOf("arrow", false, true, false, true, false));
        assertEquals("PROJECTILE", DamageCauses.causeOf("trident", false, true, false, true, false));
        assertEquals("MAGIC", DamageCauses.causeOf("indirect_magic", false, true, false, true, true));
        assertEquals("ENTITY_ATTACK", DamageCauses.causeOf("player_attack", false, true, false, false, false));
        assertEquals("ENTITY_ATTACK", DamageCauses.causeOf("mob_attack", false, true, false, false, false));
        assertEquals("ENTITY_ATTACK", DamageCauses.causeOf("sting", false, true, false, false, false));
        assertEquals("ENTITY_ATTACK", DamageCauses.causeOf("mace_smash", false, true, false, false, false));
        assertEquals("THORNS", DamageCauses.causeOf("thorns", false, true, false, false, false));
        assertEquals("SONIC_BOOM", DamageCauses.causeOf("sonic_boom", false, true, false, false, false));
        assertEquals("FALLING_BLOCK", DamageCauses.causeOf("falling_block", false, true, false, false, false));
        assertEquals("FALLING_BLOCK", DamageCauses.causeOf("falling_anvil", false, true, false, false, false));
        assertEquals("FALLING_BLOCK", DamageCauses.causeOf("falling_stalactite", false, true, false, false, false));
        assertEquals("LIGHTNING", DamageCauses.causeOf("lightning_bolt", false, true, false, false, false));
        assertEquals("FALL", DamageCauses.causeOf("fall", false, true, false, false, false));
        assertEquals("DRAGON_BREATH", DamageCauses.causeOf("dragon_breath", false, true, false, false, false));
        assertEquals("MAGIC", DamageCauses.causeOf("magic", false, true, false, false, false));
    }

    @Test
    void resolvesWithoutEntityContext() {
        assertEquals("FALL", DamageCauses.causeOf("fall", false, false, false, false, false));
        assertEquals("FALL", DamageCauses.causeOf("ender_pearl", false, false, false, false, false));
        assertEquals("FIRE", DamageCauses.causeOf("in_fire", false, false, false, false, false));
        assertEquals("CAMPFIRE", DamageCauses.causeOf("campfire", false, false, false, false, false));
        assertEquals("FIRE_TICK", DamageCauses.causeOf("on_fire", false, false, false, false, false));
        assertEquals("LAVA", DamageCauses.causeOf("lava", false, false, false, false, false));
        assertEquals("HOT_FLOOR", DamageCauses.causeOf("hot_floor", false, false, false, false, false));
        assertEquals("SUFFOCATION", DamageCauses.causeOf("in_wall", false, false, false, false, false));
        assertEquals("CRAMMING", DamageCauses.causeOf("cramming", false, false, false, false, false));
        assertEquals("DROWNING", DamageCauses.causeOf("drown", false, false, false, false, false));
        assertEquals("STARVATION", DamageCauses.causeOf("starve", false, false, false, false, false));
        assertEquals("CONTACT", DamageCauses.causeOf("cactus", false, false, false, false, false));
        assertEquals("CONTACT", DamageCauses.causeOf("sweet_berry_bush", false, false, false, false, false));
        assertEquals("CONTACT", DamageCauses.causeOf("stalagmite", false, false, false, false, false));
        assertEquals("FLY_INTO_WALL", DamageCauses.causeOf("fly_into_wall", false, false, false, false, false));
        assertEquals("VOID", DamageCauses.causeOf("out_of_world", false, false, false, false, false));
        assertEquals("VOID", DamageCauses.causeOf("fell_out_of_world", false, false, false, false, false));
        assertEquals("KILL", DamageCauses.causeOf("generic_kill", false, false, false, false, false));
        assertEquals("MAGIC", DamageCauses.causeOf("magic", false, false, false, false, false));
        assertEquals("MAGIC", DamageCauses.causeOf("indirect_magic", false, false, false, false, false));
        assertEquals("WITHER", DamageCauses.causeOf("wither", false, false, false, false, false));
        assertEquals("DRYOUT", DamageCauses.causeOf("dry_out", false, false, false, false, false));
        assertEquals("FREEZE", DamageCauses.causeOf("freeze", false, false, false, false, false));
        assertEquals("WORLD_BORDER", DamageCauses.causeOf("outside_border", false, false, false, false, false));
        assertEquals("CUSTOM", DamageCauses.causeOf("generic", false, false, false, false, false));
        assertEquals("CUSTOM", DamageCauses.causeOf("unknown", false, false, false, false, false));
        assertEquals("CUSTOM", DamageCauses.causeOf(null, false, false, false, false, false));
        assertEquals("CUSTOM", DamageCauses.causeOf("", false, false, false, false, false));
    }

    @Test
    void resolvesSingleArgumentDelegation() {
        assertEquals("FIRE", DamageCauses.causeOf("in_fire"));
        assertEquals("FIRE", DamageCauses.causeOf("minecraft:in_fire"));
        assertEquals("FALL", DamageCauses.causeOf("fall"));
        assertEquals("FALL", DamageCauses.causeOf("minecraft:fall"));
        assertEquals("VOID", DamageCauses.causeOf("out_of_world"));
        assertEquals("LAVA", DamageCauses.causeOf("lava"));
        assertEquals("CONTACT", DamageCauses.causeOf("cactus"));
        assertEquals("CUSTOM", DamageCauses.causeOf("unknown_custom"));
        assertEquals("CUSTOM", DamageCauses.causeOf(null));
        assertEquals("CUSTOM", DamageCauses.causeOf(""));
    }
}
