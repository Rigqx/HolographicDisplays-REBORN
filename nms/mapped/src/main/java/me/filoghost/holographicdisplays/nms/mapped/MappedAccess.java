/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import me.filoghost.holographicdisplays.common.PositionCoordinates;
import me.filoghost.holographicdisplays.nms.common.EntityID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Resolves Mojang names or verified Spigot aliases; loaded only on the modern adapter path. */
final class MappedAccess {

    static final MappedAccess INSTANCE = new MappedAccess();

    private final Constructor<?> spawn;
    private final Constructor<?> metadata;
    private final Constructor<?> destroy;
    private final Constructor<?> mount;
    private final Constructor<?> buffer;
    private final Constructor<?> vector;
    private final Constructor<?> teleport;
    private final Constructor<?> positionMoveRotation;
    private final Method writeVarInt;
    private final Method dataValue;
    private final Method chat;
    private final Method item;
    private final Method handle;
    private final Method send;
    private final Field playerConnection;
    private final Field networkConnection;
    private final Field channel;
    private final Field interactID;
    private final Field attackID;
    private final AtomicInteger entityCounter;

    final Object armorStand;
    final Object itemType;
    final Object slime;
    final Object sharedFlags;
    final Object customName;
    final Object nameVisible;
    final Object armorStandFlags;
    final Object itemStack;
    final Object slimeSize;

    private MappedAccess() {
        try {
            Class<?> entity = nms("world.entity.Entity");
            Class<?> entityType = nms("world.entity.EntityType");
            Class<?> vec = nms("world.phys.Vec3");
            Class<?> buf = nms("network.FriendlyByteBuf");
            spawn = nms("network.protocol.game.ClientboundAddEntityPacket").getConstructor(
                    int.class, UUID.class, double.class, double.class, double.class,
                    float.class, float.class, entityType, int.class, vec, double.class);
            metadata = nms("network.protocol.game.ClientboundSetEntityDataPacket").getConstructor(int.class, List.class);
            destroy = nms("network.protocol.game.ClientboundRemoveEntitiesPacket").getConstructor(int[].class);
            mount = constructor(nms("network.protocol.game.ClientboundSetPassengersPacket"), buf);
            buffer = buf.getConstructor(ByteBuf.class);
            vector = vec.getConstructor(double.class, double.class, double.class);
            writeVarInt = MappedNames.method(buf, "writeVarInt", int.class);
            Class<?> teleportClass = nms("network.protocol.game.ClientboundTeleportEntityPacket");
            Constructor<?> movement = null;
            Constructor<?> teleportConstructor;
            try {
                Class<?> movementClass = nms("world.entity.PositionMoveRotation");
                movement = movementClass.getConstructor(vec, vec, float.class, float.class);
                teleportConstructor = teleportClass.getConstructor(int.class, movementClass, java.util.Set.class, boolean.class);
            } catch (ClassNotFoundException e) {
                teleportConstructor = constructor(teleportClass, buf);
            }
            positionMoveRotation = movement;
            teleport = teleportConstructor;
            dataValue = MappedNames.method(nms("network.syncher.SynchedEntityData$DataValue"),
                    "create", nms("network.syncher.EntityDataAccessor"), Object.class);
            String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
            chat = Class.forName(craftPackage + ".util.CraftChatMessage").getMethod(
                    "fromString", String.class, boolean.class, boolean.class);
            item = Class.forName(craftPackage + ".inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);
            Class<?> craftPlayer = Class.forName(craftPackage + ".entity.CraftPlayer");
            handle = craftPlayer.getMethod("getHandle");
            playerConnection = field(nms("server.level.ServerPlayer"), "connection");
            Class<?> listener = nms("server.network.ServerCommonPacketListenerImpl");
            networkConnection = field(listener, "connection");
            channel = field(nms("network.Connection"), "channel");
            send = MappedNames.method(listener, "send", nms("network.protocol.Packet"));
            interactID = field(nms("network.protocol.game.ServerboundInteractPacket"), "entityId");
            Field attack = null;
            try {
                attack = field(nms("network.protocol.game.ServerboundAttackPacket"), "entityId");
            } catch (ClassNotFoundException e) {
                // Before the split, ServerboundInteractPacket includes attacks.
            }
            attackID = attack;
            AtomicInteger counter;
            try {
                counter = (AtomicInteger) field(entity, "ENTITY_COUNTER").get(null);
            } catch (NoSuchFieldException e) {
                counter = (AtomicInteger) field(nms("server.level.ServerLevel"), "ENTITY_COUNTER").get(null);
            }
            entityCounter = counter;
            armorStand = entityType(entityType, "ARMOR_STAND");
            itemType = entityType(entityType, "ITEM");
            slime = entityType(entityType, "SLIME");
            sharedFlags = field(entity, "DATA_SHARED_FLAGS_ID").get(null);
            customName = field(entity, "DATA_CUSTOM_NAME").get(null);
            nameVisible = field(entity, "DATA_CUSTOM_NAME_VISIBLE").get(null);
            armorStandFlags = field(nms("world.entity.decoration.ArmorStand"), "DATA_CLIENT_FLAGS").get(null);
            itemStack = field(nms("world.entity.item.ItemEntity"), "DATA_ITEM").get(null);
            Class<?> slimeClass;
            try {
                slimeClass = nms("world.entity.monster.Slime");
            } catch (ClassNotFoundException e) {
                slimeClass = nms("world.entity.monster.cubemob.AbstractCubeMob");
            }
            slimeSize = field(slimeClass, "ID_SIZE").get(null);
        } catch (ReflectiveOperationException e) {
            throw failure(e);
        }
    }

    private static Class<?> nms(String name) throws ClassNotFoundException {
        return MappedNames.type("net.minecraft." + name);
    }

    private static Object entityType(Class<?> type, String name) throws ReflectiveOperationException {
        try {
            return field(type, name).get(null);
        } catch (NoSuchFieldException e) {
            // 26.2 moved the constants to resource keys in EntityTypeIds.
            Object key = field(nms("world.entity.EntityTypeIds"), name).get(null);
            Object registry = field(nms("core.registries.BuiltInRegistries"), "ENTITY_TYPE").get(null);
            return MappedNames.method(nms("core.Registry"), "getValueOrThrow", nms("resources.ResourceKey")).invoke(registry, key);
        }
    }

    private static Constructor<?> constructor(Class<?> type, Class<?> argument) throws NoSuchMethodException {
        Constructor<?> result = type.getDeclaredConstructor(argument);
        result.setAccessible(true);
        return result;
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        return MappedNames.field(type, name);
    }

    static IllegalStateException failure(ReflectiveOperationException e) {
        return new IllegalStateException("Could not access the selected Mojang-mapped server implementation", e);
    }

    EntityID newEntityID() {
        return new EntityID(entityCounter::incrementAndGet);
    }

    Object spawn(EntityID id, Object type, PositionCoordinates position, double offsetY) {
        try {
            return spawn.newInstance(id.getNumericID(), id.getUUID(), position.getX(), position.getY() + offsetY,
                    position.getZ(), 0.0F, 0.0F, type, type == itemType ? 1 : 0, vector.newInstance(0.0, 0.0, 0.0), 0.0);
        } catch (ReflectiveOperationException e) {
            throw failure(e);
        }
    }

    Object teleport(EntityID id, PositionCoordinates position, double offsetY) {
        try {
            if (positionMoveRotation != null) {
                Object movement = positionMoveRotation.newInstance(
                        vector.newInstance(position.getX(), position.getY() + offsetY, position.getZ()),
                        vector.newInstance(0.0, 0.0, 0.0), 0.0F, 0.0F);
                return teleport.newInstance(id.getNumericID(), movement, Collections.emptySet(), false);
            }
            ByteBuf bytes = Unpooled.buffer();
            try {
                Object buf = buffer.newInstance(bytes);
                writeVarInt.invoke(buf, id.getNumericID());
                bytes.writeDouble(position.getX()).writeDouble(position.getY() + offsetY).writeDouble(position.getZ());
                bytes.writeByte(0).writeByte(0).writeBoolean(false);
                return teleport.newInstance(buf);
            } finally {
                bytes.release();
            }
        } catch (ReflectiveOperationException e) {
            throw failure(e);
        }
    }

    Object mount(EntityID vehicle, EntityID passenger) {
        ByteBuf bytes = Unpooled.buffer();
        try {
            Object buf = buffer.newInstance(bytes);
            writeVarInt.invoke(buf, vehicle.getNumericID());
            writeVarInt.invoke(buf, 1);
            writeVarInt.invoke(buf, passenger.getNumericID());
            return mount.newInstance(buf);
        } catch (ReflectiveOperationException e) {
            throw failure(e);
        } finally {
            bytes.release();
        }
    }

    Object metadata(EntityID id, List<Object> values) {
        try {
            return metadata.newInstance(id.getNumericID(), values);
        } catch (ReflectiveOperationException e) {
            throw failure(e);
        }
    }

    Object destroy(int[] ids) {
        try {
            return destroy.newInstance((Object) ids);
        } catch (ReflectiveOperationException e) {
            throw failure(e);
        }
    }

    Object value(Object key, Object value) {
        try {
            return dataValue.invoke(null, key, value);
        } catch (ReflectiveOperationException e) {
            throw failure(e);
        }
    }

    Object chat(String text) {
        try {
            return Array.get(chat.invoke(null, text, false, true), 0);
        } catch (ReflectiveOperationException e) {
            throw failure(e);
        }
    }

    Object item(ItemStack stack) {
        try {
            return item.invoke(null, stack);
        } catch (ReflectiveOperationException e) {
            throw failure(e);
        }
    }

    private Object connection(Player player) throws ReflectiveOperationException {
        return playerConnection.get(handle.invoke(player));
    }

    Channel channel(Player player) throws ReflectiveOperationException {
        return (Channel) channel.get(networkConnection.get(connection(player)));
    }

    void send(Player player, Object packet) {
        try {
            send.invoke(connection(player), packet);
        } catch (ReflectiveOperationException e) {
            throw failure(e);
        }
    }

    Integer interactedEntity(Object packet) throws IllegalAccessException {
        if (interactID.getDeclaringClass().isInstance(packet)) {
            return (Integer) interactID.get(packet);
        }
        if (attackID != null && attackID.getDeclaringClass().isInstance(packet)) {
            return (Integer) attackID.get(packet);
        }
        return null;
    }

}
