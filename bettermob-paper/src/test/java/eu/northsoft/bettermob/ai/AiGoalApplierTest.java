package eu.northsoft.bettermob.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiGoalApplierTest {
    @SuppressWarnings("unchecked")
    private static Goal<Mob> goal(String key) {
        Goal<Mob> goal = mock(Goal.class);
        GoalKey<Mob> goalKey = GoalKey.of(Mob.class, NamespacedKey.minecraft(key));
        when(goal.getKey()).thenReturn(goalKey);
        return goal;
    }

    @Test
    void meleeAttackMatchesZombieAttack() {
        Goal<Mob> stroll = goal("water_avoiding_random_stroll");
        Goal<Mob> zombieAttack = goal("zombie_attack");
        List<Goal<Mob>> snapshot = List.of(stroll, zombieAttack);

        assertSame(zombieAttack, AiGoalApplier.findByName(snapshot, "meleeattack"));
    }

    @Test
    void meleeAttackMatchesOtherMobSpecificAttackGoals() {
        Goal<Mob> spiderAttack = goal("spider_attack");
        Goal<Mob> drownedAttack = goal("drowned_attack");
        Goal<Mob> genericMelee = goal("melee_attack");

        assertSame(spiderAttack, AiGoalApplier.findByName(List.of(spiderAttack), "meleeattack"));
        assertSame(drownedAttack, AiGoalApplier.findByName(List.of(drownedAttack), "meleeattack"));
        assertSame(genericMelee, AiGoalApplier.findByName(List.of(genericMelee), "meleeattack"));
    }

    @Test
    void meleeAttackDoesNotMatchRangedGoals() {
        Goal<Mob> bow = goal("ranged_bow_attack");
        assertNull(AiGoalApplier.findByName(List.of(bow), "meleeattack"));
    }

    @Test
    void attackerMatchesHurtBy() {
        Goal<Mob> hurtBy = goal("hurt_by");
        assertSame(hurtBy, AiGoalApplier.findByName(List.of(hurtBy), "attacker"));
    }
}
