package eu.northsoft.bettermob.fabric;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FabricTriggerGuardTest {
    private final FabricTriggerGuard guard = new FabricTriggerGuard();

    @Test
    void blocksReentryUntilOuterActionCompletes() {
        assertTrue(guard.run("mob:HEAL", () -> {
            assertFalse(guard.run("mob:HEAL", () -> fail("Reentrant action ran")));
            assertFalse(guard.run("mob:HEAL", () -> fail("Outer guard was released")));
        }));
        assertTrue(guard.run("mob:HEAL", () -> {}));
    }

    @Test
    void releasesKeyAfterException() {
        IllegalStateException failure = new IllegalStateException("failure");
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> guard.run("mob:HEAL", () -> { throw failure; })));
        assertTrue(guard.run("mob:HEAL", () -> {}));
    }

    @Test
    void allowsOtherTriggerTypesAndMobsWithoutReleasingOuterKey() {
        List<String> executed = new ArrayList<>();
        assertTrue(guard.run("mob:HEAL", () -> {
            assertTrue(guard.run("mob:HEALTH", () -> executed.add("otherType")));
            assertTrue(guard.run("otherMob:HEAL", () -> executed.add("otherMob")));
            assertFalse(guard.run("mob:HEAL", () -> fail("Outer guard was released")));
        }));
        assertEquals(List.of("otherType", "otherMob"), executed);
    }

    @Test
    void nestedExceptionReleasesOnlyItsOwnKey() {
        assertTrue(guard.run("mob:HEAL", () -> {
            assertThrows(IllegalStateException.class, () -> guard.run("mob:HEALTH", () -> {
                throw new IllegalStateException("failure");
            }));
            assertTrue(guard.run("mob:HEALTH", () -> {}));
            assertFalse(guard.run("mob:HEAL", () -> fail("Outer guard was released")));
        }));
        assertTrue(guard.run("mob:HEAL", () -> {}));
    }
}
