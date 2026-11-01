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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

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
            living.addCommandTag(FabricMobTags.tag(definition.id));
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
        boolean explosion = source.isIn(DamageTypeTags.IS_EXPLOSION);
        Entity direct = source.getSource();
        boolean directEntity = direct != null;
        boolean tnt = direct instanceof TntEntity;
        boolean projectile = direct instanceof ProjectileEntity;
        boolean potion = direct instanceof PotionEntity;
        String damageTypeId = source.getTypeRegistryEntry().getKey()
                .map(key -> key.getValue().toString())
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
        ActiveFabricMob mob = activeMobs.get(entity.getUuid());
        if (mob == null) return;
        CoreSkillContext ctx = new CoreSkillContext(mob.entity(), null);
        triggerGuard.run(entity.getUuid() + ":HEAL", () -> {
            for (var trigger : mob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.HEAL)) {
                skillEngine.executeStep(trigger.step(), ctx);
            }
        });
        mob.checkPhases(skillEngine, mod);
        var crossed = mob.crossedHealthTriggers(entity.getHealth(), entity.getMaxHealth());
        triggerGuard.run(entity.getUuid() + ":HEALTH", () -> {
            for (var trigger : crossed) {
                skillEngine.executeStep(trigger.step(), ctx);
            }
        });
    }

    public void registerEvents() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
            ActiveFabricMob mob = activeMobs.get(victim.getUuid());
            if (mob == null) return true;
            double modifier = modifierFor(mob, source);
            return !(modifier <= 0.0);
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamageTaken, damageTaken, blocked) -> {
            LivingEntity attacker = resolveAttacker(source);
            ActiveFabricMob victimMob = activeMobs.get(victim.getUuid());
            if (victimMob != null) {
                CoreSkillContext ctx = new CoreSkillContext(
                        victimMob.entity(),
                        attacker != null ? FabricAdapters.living(attacker) : null
                );
                triggerGuard.run(victim.getUuid() + ":DAMAGED", () -> {
                    for (var trigger : victimMob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.DAMAGED)) {
                        skillEngine.executeStep(trigger.step(), ctx);
                    }
                });
                victimMob.checkPhases(skillEngine, mod);
                var crossed = victimMob.crossedHealthTriggers(victim.getHealth(), victim.getMaxHealth());
                triggerGuard.run(victim.getUuid() + ":HEALTH", () -> {
                    for (var trigger : crossed) {
                        skillEngine.executeStep(trigger.step(), ctx);
                    }
                });
            }

            if (attacker != null) {
                ActiveFabricMob attackerMob = activeMobs.get(attacker.getUuid());
                if (attackerMob != null) {
                    CoreSkillContext ctx = new CoreSkillContext(
                            attackerMob.entity(),
                            FabricAdapters.living(victim)
                    );
                    triggerGuard.run(attacker.getUuid() + ":ATTACK", () -> {
                        for (var trigger : attackerMob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.ATTACK)) {
                            skillEngine.executeStep(trigger.step(), ctx);
                        }
                    });
                }
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            ActiveFabricMob mob = activeMobs.remove(victim.getUuid());
            if (mob == null) return;
            LivingEntity killer = resolveAttacker(source);
            CoreSkillContext ctx = new CoreSkillContext(
                    mob.entity(),
                    killer != null ? FabricAdapters.living(killer) : null
            );
            triggerGuard.run(victim.getUuid() + ":DEATH", () -> {
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
            if (hand != Hand.MAIN_HAND || !(entity instanceof LivingEntity living)) return ActionResult.PASS;
            ActiveFabricMob mob = activeMobs.get(entity.getUuid());
            if (mob == null) return ActionResult.PASS;
            if (mob.definition().options != null && !mob.definition().options.interactable()) {
                return ActionResult.FAIL;
            }
            CoreSkillContext ctx = new CoreSkillContext(
                    mob.entity(),
                    FabricAdapters.living(player)
            );
            triggerGuard.run(entity.getUuid() + ":INTERACT", () -> {
                for (var trigger : mob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.INTERACT)) {
                    skillEngine.executeStep(trigger.step(), ctx);
                }
            });
            return ActionResult.PASS;
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
            ActiveFabricMob existing = activeMobs.get(entity.getUuid());
            if (existing != null && existing.entity().handle() == entity) return;
            String id = FabricMobTags.mobId(entity.getCommandTags());
            if (id == null) return;
            MobDefinition definition = mod.mobRegistry().get(id);
            if (definition == null) return;
            PlatformLivingEntity adapted = FabricAdapters.living(living);
            ActiveFabricMob mob = new ActiveFabricMob(entity.getUuid(), definition, adapted);
            activeMobs.put(entity.getUuid(), mob);
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
            ActiveFabricMob mob = activeMobs.get(entity.getUuid());
            if (mob != null && mob.entity().handle() == entity) {
                activeMobs.remove(entity.getUuid(), mob);
            }
        });
    }

    private static LivingEntity resolveAttacker(DamageSource source) {
        if (source == null) return null;
        Entity attacker = source.getAttacker();
        if (attacker instanceof ProjectileEntity proj && proj.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        return attacker instanceof LivingEntity living ? living : null;
    }
}
