package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformWorld;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.platform.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class FabricEntity implements PlatformEntity {
    protected final Entity entity;

    public FabricEntity(Entity entity) {
        this.entity = entity;
    }

    @Override
    public UUID uuid() {
        return entity.getUUID();
    }

    @Override
    public String typeKey() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    @Override
    public Position position() {
        String worldKey = entity.level() instanceof ServerLevel sw
                ? sw.dimension().identifier().toString()
                : "minecraft:overworld";
        return new Position(worldKey, entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
    }

    @Override
    public PlatformWorld world() {
        return entity.level() instanceof ServerLevel sw ? new FabricWorld(sw) : null;
    }

    @Override
    public boolean isValid() {
        return entity.isAlive() && !entity.isRemoved();
    }

    @Override
    public void teleport(Position position) {
        entity.teleportTo(position.x(), position.y(), position.z());
    }

    @Override
    public Vec3 velocity() {
        return FabricAdapters.vec(entity.getDeltaMovement());
    }

    @Override
    public void velocity(Vec3 velocity) {
        entity.setDeltaMovement(FabricAdapters.mcVec(velocity));
    }

    @Override
    public void remove() {
        entity.discard();
    }

    @Override
    public String customName() {
        Component text = entity.getCustomName();
        return text != null ? text.getString() : null;
    }

    @Override
    public void customName(String name) {
        if (name == null) {
            entity.setCustomName(null);
        } else {
            entity.setCustomName(Component.literal(name));
        }
    }

    @Override
    public boolean isCustomNameVisible() {
        return entity.isCustomNameVisible();
    }

    @Override
    public void customNameVisible(boolean visible) {
        entity.setCustomNameVisible(visible);
    }

    @Override
    public boolean isPlayer() {
        return entity instanceof Player;
    }

    @Override
    public void sendMessage(String message) {
        if (message != null && entity instanceof Player pe) {
            pe.sendSystemMessage(Component.literal(message.replace('&', '§')));
        }
    }

    @Override
    public void sendActionBar(String message) {
        if (message != null && entity instanceof Player pe) {
            pe.sendOverlayMessage(Component.literal(message.replace('&', '§')));
        }
    }

    @Override
    public void setFireTicks(int ticks) {
        entity.setRemainingFireTicks(ticks);
    }

    @Override
    public Entity handle() {
        return entity;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FabricEntity fe && fe.entity.getUUID().equals(entity.getUUID());
    }

    @Override
    public int hashCode() {
        return entity.getUUID().hashCode();
    }
}
