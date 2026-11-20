package eu.northsoft.bettermob.fabric.mixin;

import eu.northsoft.bettermob.fabric.ActiveFabricMob;
import eu.northsoft.bettermob.fabric.BetterMobFabricMod;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
    @Inject(method = "tickBurnInDaylight", at = @At("HEAD"), cancellable = true)
    private void bettermob$preventSunburn(CallbackInfo ci) {
        if (BetterMobFabricMod.instance() == null) return;
        MobEntity entity = (MobEntity) (Object) this;
        ActiveFabricMob mob = BetterMobFabricMod.instance().mobManager().get(entity.getUuid());
        if (mob != null && mob.definition().options != null && mob.definition().options.preventSunburn()) {
            ci.cancel();
        }
    }
}
