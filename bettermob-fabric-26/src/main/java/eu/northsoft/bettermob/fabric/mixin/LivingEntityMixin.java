package eu.northsoft.bettermob.fabric.mixin;

import eu.northsoft.bettermob.fabric.ActiveFabricMob;
import eu.northsoft.bettermob.fabric.BetterMobFabricMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private float bettermob$healthBeforeHeal;

    @Inject(method = "heal", at = @At("HEAD"))
    private void bettermob$captureHealthBefore(float amount, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        this.bettermob$healthBeforeHeal = entity.getHealth();
    }

    @Inject(method = "heal", at = @At("TAIL"))
    private void bettermob$onHeal(float amount, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide() && BetterMobFabricMod.instance() != null) {
            float actualHealed = entity.getHealth() - this.bettermob$healthBeforeHeal;
            if (actualHealed > 0.0f) {
                BetterMobFabricMod.instance().mobManager().onHeal(entity, actualHealed);
            }
        }
    }

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float bettermob$modifyDamage(float amount, ServerLevel level, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!level.isClientSide() && BetterMobFabricMod.instance() != null) {
            ActiveFabricMob mob = BetterMobFabricMod.instance().mobManager().get(entity.getUUID());
            if (mob != null) {
                double modifier = BetterMobFabricMod.instance().mobManager().modifierFor(mob, source);
                if (Double.isFinite(modifier) && modifier != 1.0) {
                    return (float) (amount * modifier);
                }
            }
        }
        return amount;
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void bettermob$dropAllDeathLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if (BetterMobFabricMod.instance() != null) {
            ActiveFabricMob mob = BetterMobFabricMod.instance().mobManager().get(((LivingEntity) (Object) this).getUUID());
            if (mob != null) {
                boolean prevent = mob.definition().drops != null
                        || (mob.definition().options != null && mob.definition().options.preventOtherDrops());
                if (prevent) {
                    ci.cancel();
                }
            }
        }
    }
}
