package eu.northsoft.bettermob.fabric;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class FabricSpawnerEvents {
    private FabricSpawnerEvents() {}

    public static void register(FabricSpawnerManager spawners) {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof LivingEntity) spawners.track(entity.getUUID(), FabricAdapters.tags(entity));
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (destroyed(entity)) spawners.untrack(entity.getUUID());
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) ->
                spawners.untrack(entity.getUUID()));
    }

    private static boolean destroyed(Entity entity) {
        Entity.RemovalReason reason = entity.getRemovalReason();
        return reason != null && reason.shouldDestroy();
    }
}
