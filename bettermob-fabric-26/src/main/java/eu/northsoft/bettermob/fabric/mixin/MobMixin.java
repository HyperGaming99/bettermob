package eu.northsoft.bettermob.fabric.mixin;

import eu.northsoft.bettermob.fabric.ActiveFabricMob;
import eu.northsoft.bettermob.fabric.BetterMobFabricMod;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "burnUndead", at = @At("HEAD"), cancellable = true)
    private void bettermob$preventSunburn(CallbackInfo ci) {
        if (BetterMobFabricMod.instance() == null) return;
        Mob entity = (Mob) (Object) this;
        ActiveFabricMob mob = BetterMobFabricMod.instance().mobManager().get(entity.getUUID());
        if (mob != null && mob.definition().options != null && mob.definition().options.preventSunburn()) {
            ci.cancel();
        }
    }
}
