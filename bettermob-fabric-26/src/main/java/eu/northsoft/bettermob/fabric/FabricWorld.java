package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformWorld;
import eu.northsoft.bettermob.platform.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class FabricWorld implements PlatformWorld {
    private final ServerLevel world;

    public FabricWorld(ServerLevel world) {
        this.world = world;
    }

    @Override
    public String key() {
        return world.dimension().identifier().toString();
    }

    @Override
    public long time() {
        return world.getOverworldClockTime();
    }

    @Override
    public Optional<PlatformEntity> spawn(String typeKey, Position position) {
        if (typeKey == null) return Optional.empty();
        String normalized = typeKey.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.contains(":")) normalized = "minecraft:" + normalized;
        Identifier id = Identifier.tryParse(normalized);
        if (id == null) return Optional.empty();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) return Optional.empty();

        BlockPos pos = new BlockPos(position.blockX(), position.blockY(), position.blockZ());
        Entity spawned = type.spawn(world, pos, EntitySpawnReason.COMMAND);
        if (spawned != null) {
            spawned.snapTo(position.x(), position.y(), position.z(), position.yaw(), position.pitch());
            return Optional.of(FabricAdapters.entity(spawned));
        }
        return Optional.empty();
    }

    @Override
    public Optional<PlatformEntity> entity(UUID uuid) {
        return Optional.ofNullable(world.getEntity(uuid)).map(FabricAdapters::entity);
    }

    @Override
    public Collection<PlatformEntity> nearbyEntities(Position center, double radius) {
        AABB box = new AABB(center.x() - radius, center.y() - radius, center.z() - radius,
                            center.x() + radius, center.y() + radius, center.z() + radius);
        return world.getEntities((Entity) null, box, e -> true).stream()
                .map(FabricAdapters::entity)
                .toList();
    }

    @Override
    public String blockKey(int x, int y, int z) {
        return BuiltInRegistries.BLOCK.getKey(world.getBlockState(new BlockPos(x, y, z)).getBlock()).toString();
    }

    @Override
    public void playSound(Position position, String soundKey, float volume, float pitch) {
        if (soundKey == null) return;
        String normalized = soundKey.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.contains(":")) normalized = "minecraft:" + normalized;
        Identifier id = Identifier.tryParse(normalized);
        if (id == null) return;
        var soundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(null);
        if (soundEvent != null) {
            world.playSound(null, position.x(), position.y(), position.z(), soundEvent, SoundSource.HOSTILE, volume, pitch);
        }
    }

    @Override
    public void strikeLightning(Position position, boolean damage) {
        LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
        if (lightning != null) {
            lightning.snapTo(position.x(), position.y(), position.z(), 0, 0);
            if (!damage) {
                lightning.setVisualOnly(true);
            }
            world.addFreshEntity(lightning);
        }
    }

    @Override
    public Optional<PlatformEntity> nearestPlayer(Position center, double maxDistance) {
        Player player = world.getNearestPlayer(center.x(), center.y(), center.z(), maxDistance, false);
        return Optional.ofNullable(player).map(FabricAdapters::entity);
    }

    @Override
    public void dropItem(Position position, String itemType, int count) {
        if (itemType == null || count <= 0) return;
        if (BetterMobFabricMod.instance() != null && BetterMobFabricMod.instance().dropRegistry() != null) {
            BetterMobFabricMod.instance().dropRegistry().spawnDrop(world, position, itemType, count);
            return;
        }
        String normalized = itemType.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.contains(":")) normalized = "minecraft:" + normalized;
        Identifier id = Identifier.tryParse(normalized);
        if (id == null) return;
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item != null && item != Items.AIR) {
            ItemEntity itemEntity = new ItemEntity(
                    world, position.x(), position.y(), position.z(), new ItemStack(item, count));
            world.addFreshEntity(itemEntity);
        }
    }

    @Override
    public void dropExperience(Position position, int amount) {
        if (amount <= 0) return;
        ExperienceOrb.award(world, new Vec3(position.x(), position.y(), position.z()), amount);
    }

    @Override
    public ServerLevel handle() {
        return world;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FabricWorld fw && fw.world.dimension().equals(world.dimension());
    }

    @Override
    public int hashCode() {
        return world.dimension().hashCode();
    }
}
