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

    public static void addTag(PlatformEntity entity, String tag) {
        if (entity != null && tag != null && entity.handle() instanceof Entity mc) {
            mc.addTag(tag);
        }
    }

    public static java.util.Set<String> tags(Entity entity) {
        return entity == null ? java.util.Set.of() : entity.entityTags();
    }

    public static boolean entitiesLoaded(eu.northsoft.bettermob.platform.PlatformWorld world, double x, double z, int radius) {
        if (world == null || !(world.handle() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        int chunkRadius = Math.max(0, radius) / 16 + 1;
        int centerX = ((int) Math.floor(x)) >> 4;
        int centerZ = ((int) Math.floor(z)) >> 4;
        for (int cx = centerX - chunkRadius; cx <= centerX + chunkRadius; cx++) {
            for (int cz = centerZ - chunkRadius; cz <= centerZ + chunkRadius; cz++) {
                if (!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(cx, cz))) return false;
            }
        }
        return true;
    }
}
