package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class FabricAdapters {
    private FabricAdapters() {}

    public static Vec3 vec(net.minecraft.world.phys.Vec3 vec) {
        if (vec == null) return null;
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    public static net.minecraft.world.phys.Vec3 mcVec(Vec3 vec) {
        if (vec == null) return null;
        return new net.minecraft.world.phys.Vec3(vec.x(), vec.y(), vec.z());
    }

    public static PlatformEntity entity(Entity entity) {
        if (entity == null) return null;
        return entity instanceof LivingEntity living ? new FabricLivingEntity(living) : new FabricEntity(entity);
    }

    public static eu.northsoft.bettermob.platform.PlatformLivingEntity living(LivingEntity entity) {
        return entity == null ? null : new FabricLivingEntity(entity);
    }

    public static Entity minecraft(PlatformEntity entity) {
        if (entity == null) return null;
        if (entity.handle() instanceof Entity mc) return mc;
        throw new IllegalArgumentException("Not a Fabric entity: " + entity.getClass().getName());
    }
}
