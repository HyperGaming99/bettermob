package eu.northsoft.bettermob.fabric;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FabricMobTagsTest {
    @Test
    void buildsLowercasePersistentTag() {
        assertEquals("bettermob.mob.iron_golem", FabricMobTags.tag("IRON_GOLEM"));
    }

    @Test
    void preservesRegistryIdPunctuationAndSpaces() {
        String id = "pack:boss.v2-test_1 name";
        assertEquals(id, FabricMobTags.mobId(Set.of(FabricMobTags.tag(id))));
    }

    @Test
    void readsMobIdAmongUnrelatedTags() {
        assertEquals("boss", FabricMobTags.mobId(Set.of("team.red", "bettermob.mob.BOSS", "other.mob.zombie")));
    }

    @Test
    void ignoresMissingForeignAndMalformedTags() {
        assertNull(FabricMobTags.mobId(null));
        assertNull(FabricMobTags.mobId(Set.of()));
        assertNull(FabricMobTags.mobId(Arrays.asList(null, "", "bettermob.mob", "bettermob.mob.",
                "bettermob.mob.   ", "bettermob.mob.boss\n", "other.bettermob.mob.boss", "BETTERMOB.mob.boss")));
    }

    @Test
    void skipsInvalidTagsBeforeValidTag() {
        assertEquals("boss", FabricMobTags.mobId(List.of("bettermob.mob.", "bettermob.mob.\t", "bettermob.mob.boss")));
    }

    @Test
    void resolvesMultipleTagsIndependentlyOfIterationOrder() {
        assertEquals("alpha", FabricMobTags.mobId(List.of("bettermob.mob.zombie", "bettermob.mob.alpha")));
        assertEquals("alpha", FabricMobTags.mobId(List.of("bettermob.mob.alpha", "bettermob.mob.zombie")));
    }

    @Test
    void rejectsInvalidIdsWhenBuildingTags() {
        assertThrows(IllegalArgumentException.class, () -> FabricMobTags.tag(null));
        assertThrows(IllegalArgumentException.class, () -> FabricMobTags.tag(""));
        assertThrows(IllegalArgumentException.class, () -> FabricMobTags.tag("   "));
        assertThrows(IllegalArgumentException.class, () -> FabricMobTags.tag("boss\n"));
    }
}
