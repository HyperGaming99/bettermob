package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformLivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class FabricLivingEntity extends FabricEntity implements PlatformLivingEntity {
    public FabricLivingEntity(LivingEntity entity) {
        super(entity);
    }

    private LivingEntity living() {
        return (LivingEntity) entity;
    }

    @Override
    public double health() {
        return living().getHealth();
    }

    @Override
    public void health(double health) {
        living().setHealth((float) Math.max(0, Math.min(health, maxHealth())));
    }

    @Override
    public double maxHealth() {
        return living().getMaxHealth();
    }

    @Override
    public void maxHealth(double maxHealth) {
        AttributeInstance attribute = living().getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) attribute.setBaseValue(maxHealth);
    }

    @Override
    public void damage(double amount, PlatformEntity source) {
        if (living().level() instanceof ServerLevel sw) {
            if (source != null && source.handle() instanceof Entity srcEntity) {
                living().hurtServer(sw, sw.damageSources().mobAttack(srcEntity instanceof LivingEntity l ? l : null), (float) amount);
            } else {
                living().hurtServer(sw, sw.damageSources().generic(), (float) amount);
            }
        }
    }

    @Override
    public void addPotionEffect(String type, int durationTicks, int amplifier) {
        if (type == null) return;
        String normalized = type.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.contains(":")) normalized = "minecraft:" + normalized;
        Identifier id = Identifier.tryParse(normalized);
        if (id == null) return;
        var effect = BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
        if (effect != null) {
            living().addEffect(new MobEffectInstance(effect, durationTicks, amplifier));
        }
    }

    @Override
    public LivingEntity handle() {
        return living();
    }
}
