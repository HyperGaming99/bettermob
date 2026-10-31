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
