package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.mob.DamageCauses;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.platform.PlatformLivingEntity;
import eu.northsoft.bettermob.skill.SkillStep;
import eu.northsoft.bettermob.skill.core.CoreSkillContext;
import eu.northsoft.bettermob.skill.core.CoreSkillEngine;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricMobManager {
    private final BetterMobFabricMod mod;
    private final CoreSkillEngine skillEngine;
    private final FabricTriggerGuard triggerGuard = new FabricTriggerGuard();
    private final Map<UUID, ActiveFabricMob> activeMobs = new ConcurrentHashMap<>();

    public FabricMobManager(BetterMobFabricMod mod, CoreSkillEngine skillEngine) {
        this.mod = mod;
        this.skillEngine = skillEngine;
    }

    public void register(UUID uuid, MobDefinition definition, PlatformLivingEntity entity) {
        if (entity.handle() instanceof LivingEntity living) {
            living.addTag(FabricMobTags.tag(definition.id));
        }
        ActiveFabricMob mob = new ActiveFabricMob(uuid, definition, entity);
        activeMobs.put(uuid, mob);

        CoreSkillContext ctx = new CoreSkillContext(entity);
        triggerGuard.run(uuid + ":SPAWN", () -> {
            for (var trigger : definition.triggersOf(MobDefinition.SkillTrigger.Trigger.SPAWN)) {
                skillEngine.executeStep(trigger.step(), ctx);
            }
        });
    }

    public ActiveFabricMob get(UUID uuid) {
        return activeMobs.get(uuid);
    }

    public void unregister(UUID uuid) {
        activeMobs.remove(uuid);
    }

    public Map<UUID, ActiveFabricMob> activeMobs() {
        return activeMobs;
    }

    public double modifierFor(UUID uuid, DamageSource source) {
        if (uuid == null || source == null) return 1.0;
        return modifierFor(activeMobs.get(uuid), source);
    }

    public double modifierFor(ActiveFabricMob mob, DamageSource source) {
        if (mob == null || mob.definition() == null || mob.definition().damageModifiers == null || source == null) {
            return 1.0;
        }
        boolean explosion = source.is(DamageTypeTags.IS_EXPLOSION);
        Entity direct = source.getDirectEntity();
        boolean directEntity = direct != null;
        boolean tnt = direct instanceof PrimedTnt;
        boolean projectile = direct instanceof Projectile;
        boolean potion = direct instanceof AbstractThrownPotion;
        String damageTypeId = source.typeHolder().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(null);
        String cause = DamageCauses.causeOf(damageTypeId, explosion, directEntity, tnt, projectile, potion);
        Double modifier = mob.definition().damageModifiers.get(cause);
        if (modifier == null || !Double.isFinite(modifier)) {
            return 1.0;
        }
        return modifier;
    }

    public int livingOf(java.util.Set<String> mobIds) {
        if (mobIds == null || mobIds.isEmpty() || activeMobs.isEmpty()) return 0;
        int count = 0;
        for (ActiveFabricMob mob : activeMobs.values()) {
            if (mob.definition() != null && mobIds.contains(mob.definition().id.toLowerCase(java.util.Locale.ROOT))) {
                count++;
            }
        }
        return count;
    }

    public int reloadLiving(FabricMobRegistry registry) {
        int updated = 0;
        for (ActiveFabricMob mob : activeMobs.values()) {
            if (mob.definition() != null) {
                MobDefinition newDef = registry.get(mob.definition().id);
                if (newDef != null) {
                    mob.updateDefinition(newDef);
                    updated++;
                }
            }
        }
        return updated;
    }

    public void onHeal(LivingEntity entity, float amount) {
        if (entity == null || amount <= 0) return;
        ActiveFabricMob mob = activeMobs.get(entity.getUUID());
        if (mob == null) return;
        CoreSkillContext ctx = new CoreSkillContext(mob.entity(), null);
        triggerGuard.run(entity.getUUID() + ":HEAL", () -> {
            for (var trigger : mob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.HEAL)) {
                skillEngine.executeStep(trigger.step(), ctx);
            }
        });
        mob.checkPhases(skillEngine, mod);
        triggerGuard.run(entity.getUUID() + ":HEALTH", () -> {
            for (var trigger : mob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.HEALTH)) {
                if (trigger.health() != null && trigger.health().matches(entity.getHealth(), entity.getMaxHealth())) {
                    skillEngine.executeStep(trigger.step(), ctx);
                }
            }
        });
    }

    public void registerEvents() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
            ActiveFabricMob mob = activeMobs.get(victim.getUUID());
            if (mob == null) return true;
            double modifier = modifierFor(mob, source);
            return !(modifier <= 0.0);
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamageTaken, damageTaken, blocked) -> {
            LivingEntity attacker = resolveAttacker(source);
            ActiveFabricMob victimMob = activeMobs.get(victim.getUUID());
            if (victimMob != null) {
                CoreSkillContext ctx = new CoreSkillContext(
                        victimMob.entity(),
                        attacker != null ? FabricAdapters.living(attacker) : null
                );
                triggerGuard.run(victim.getUUID() + ":DAMAGED", () -> {
                    for (var trigger : victimMob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.DAMAGED)) {
                        skillEngine.executeStep(trigger.step(), ctx);
                    }
                });
                victimMob.checkPhases(skillEngine, mod);
                triggerGuard.run(victim.getUUID() + ":HEALTH", () -> {
                    for (var trigger : victimMob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.HEALTH)) {
                        if (trigger.health() != null && trigger.health().matches(victim.getHealth(), victim.getMaxHealth())) {
                            skillEngine.executeStep(trigger.step(), ctx);
                        }
                    }
                });
            }

            if (attacker != null) {
                ActiveFabricMob attackerMob = activeMobs.get(attacker.getUUID());
                if (attackerMob != null) {
                    CoreSkillContext ctx = new CoreSkillContext(
                            attackerMob.entity(),
                            FabricAdapters.living(victim)
                    );
                    triggerGuard.run(attacker.getUUID() + ":ATTACK", () -> {
                        for (var trigger : attackerMob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.ATTACK)) {
                            skillEngine.executeStep(trigger.step(), ctx);
                        }
                    });
                }
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            ActiveFabricMob mob = activeMobs.remove(victim.getUUID());
            if (mob == null) return;
            LivingEntity killer = resolveAttacker(source);
            CoreSkillContext ctx = new CoreSkillContext(
                    mob.entity(),
                    killer != null ? FabricAdapters.living(killer) : null
            );
            triggerGuard.run(victim.getUUID() + ":DEATH", () -> {
                for (var trigger : mob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.DEATH)) {
                    skillEngine.executeStep(trigger.step(), ctx);
                }
            });
            if (mob.definition().drops != null && mob.entity() != null && mob.entity().world() != null) {
                var dropResult = mod.dropRegistry() != null
                        ? mod.dropRegistry().roll(mob.definition().drops)
                        : eu.northsoft.bettermob.drop.DropRoller.roll(mob.definition().drops);
                for (var drop : dropResult.items()) {
                    mob.entity().world().dropItem(mob.entity().position(), drop.itemKey(), drop.amount());
                }
                if (dropResult.exp() > 0) {
                    mob.entity().world().dropExperience(mob.entity().position(), dropResult.exp());
                }
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND || !(entity instanceof LivingEntity living)) return InteractionResult.PASS;
            ActiveFabricMob mob = activeMobs.get(entity.getUUID());
            if (mob == null) return InteractionResult.PASS;
            if (mob.definition().options != null && !mob.definition().options.interactable()) {
                return InteractionResult.FAIL;
            }
            CoreSkillContext ctx = new CoreSkillContext(
                    mob.entity(),
                    FabricAdapters.living(player)
            );
            triggerGuard.run(entity.getUUID() + ":INTERACT", () -> {
                for (var trigger : mob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.INTERACT)) {
                    skillEngine.executeStep(trigger.step(), ctx);
                }
            });
            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (mod.spawnerManager() != null) {
                mod.spawnerManager().tick();
            }
            if (activeMobs.isEmpty()) return;
            var iterator = activeMobs.values().iterator();
            while (iterator.hasNext()) {
                ActiveFabricMob mob = iterator.next();
                if (!mob.entity().isValid()) {
                    iterator.remove();
                    continue;
                }
                mob.tick();
                triggerGuard.run(mob.entity().uuid() + ":TIMER", () -> {
                    for (var trigger : mob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.TIMER)) {
                        int interval = trigger.timerTicks() > 0 ? trigger.timerTicks() : 20;
                        if (mob.ticksAlive() % interval == 0) {
                            skillEngine.executeStep(trigger.step(), new CoreSkillContext(mob.entity()));
                        }
                    }
                });
            }
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof LivingEntity living)) return;
            ActiveFabricMob existing = activeMobs.get(entity.getUUID());
            if (existing != null && existing.entity().handle() == entity) return;
            String id = FabricMobTags.mobId(entity.entityTags());
            if (id == null) return;
            MobDefinition definition = mod.mobRegistry().get(id);
            if (definition == null) return;
            PlatformLivingEntity adapted = FabricAdapters.living(living);
            ActiveFabricMob mob = new ActiveFabricMob(entity.getUUID(), definition, adapted);
            activeMobs.put(entity.getUUID(), mob);
            boolean hasLoadModel = definition.triggersOf(MobDefinition.SkillTrigger.Trigger.LOAD).stream()
                    .anyMatch(trigger -> trigger.step() instanceof SkillStep.Mechanic mechanic
                            && mechanic.name().equals("model"));
            if (definition.modelId != null && !hasLoadModel) {
                mod.platform().model("bettermodel").ifPresent(model -> model.attach(adapted, definition.modelId));
            }
            mob.restorePhase(mod);
            CoreSkillContext ctx = new CoreSkillContext(adapted);
            for (var trigger : definition.triggersOf(MobDefinition.SkillTrigger.Trigger.LOAD)) {
                skillEngine.executeStep(trigger.step(), ctx);
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            ActiveFabricMob mob = activeMobs.get(entity.getUUID());
            if (mob != null && mob.entity().handle() == entity) {
                activeMobs.remove(entity.getUUID(), mob);
            }
        });
    }

    private static LivingEntity resolveAttacker(DamageSource source) {
        if (source == null) return null;
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) {
            return living;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile proj && proj.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        return null;
    }
}
