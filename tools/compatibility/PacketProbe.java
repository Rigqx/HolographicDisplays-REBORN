/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.bukkit.craftbukkit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.filoghost.holographicdisplays.common.PositionCoordinates;
import me.filoghost.holographicdisplays.nms.common.PacketGroup;
import me.filoghost.holographicdisplays.nms.common.entity.ClickableNMSPacketEntity;
import me.filoghost.holographicdisplays.nms.common.entity.ItemNMSPacketEntity;
import me.filoghost.holographicdisplays.nms.common.entity.TextNMSPacketEntity;
import me.filoghost.holographicdisplays.nms.mapped.VersionNMSManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Standalone native-packet probe. Does not start a server or accept the Minecraft EULA. */
public class PacketProbe {

    interface ServerPackageMarker {}

    private static Object registryAccess;
    private static int checked;
    private static boolean metadataCodecUnavailable;
    private static boolean itemMetadataCodecUnavailable;

    public static void main(String[] args) throws Exception {
        try {
            run(args);
        } catch (Throwable failure) {
            try (java.io.PrintWriter out = new java.io.PrintWriter("probe-error.log")) {
                failure.printStackTrace(out);
            }
            throw failure;
        }
    }

    private static void run(String[] args) throws Exception {
        String craftPackage = args.length > 1 ? args[1] : "org.bukkit.craftbukkit";
        String bukkitVersion = (String) Class.forName(craftPackage + ".util.Versioning")
                .getMethod("getBukkitVersion").invoke(null);
        Server server = (Server) Proxy.newProxyInstance(PacketProbe.class.getClassLoader(),
                new Class<?>[] {Server.class, args.length > 1 ? Class.forName(args[1] + ".ProbeServerMarker") : ServerPackageMarker.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getLogger")) {
                        return Logger.getLogger("PacketProbe");
                    }
                    if (method.getName().equals("getName") || method.getName().equals("getVersion")) {
                        return "PacketProbe";
                    }
                    if (method.getName().equals("getBukkitVersion")) {
                        return bukkitVersion;
                    }
                    if (method.getName().equals("getUnsafe")) {
                        return Class.forName(craftPackage + ".util.CraftMagicNumbers").getField("INSTANCE").get(null);
                    }
                    if (method.getName().equals("getItemFactory")) {
                        return Class.forName(craftPackage + ".inventory.CraftItemFactory").getMethod("instance").invoke(null);
                    }
                    if (method.getName().equals("getRegistry")) {
                        try {
                            return Class.forName(craftPackage + ".CraftRegistry").getMethod("createRegistry",
                                    Class.class, Class.forName("net.minecraft.core.RegistryAccess"))
                                    .invoke(null, arguments[0], registryAccess);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            // The probe has no world registries. Built-in item/attribute registries are available.
                            return null;
                        }
                    }
                    throw new UnsupportedOperationException("Probe server: " + method);
                });
        Field bukkitServer = Bukkit.class.getDeclaredField("server");
        bukkitServer.setAccessible(true);
        bukkitServer.set(null, server);
        Object selectedVersion = Class.forName("me.filoghost.holographicdisplays.core.NMSVersion")
                .getMethod("getCurrent").invoke(null);
        require(selectedVersion.toString().equals("MAPPED"), "Native Bukkit version selects modern adapter: " + bukkitVersion);
        Object config = null;
        try {
            Class<?> configuration = Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
            config = configuration.getConstructor().newInstance();
            Field configurationInstance = configuration.getDeclaredField("instance");
            configurationInstance.setAccessible(true);
            configurationInstance.set(null, config);
        } catch (ClassNotFoundException e) {
            // Spigot has no Paper configuration.
        }
        method(type("net.minecraft.SharedConstants"), "tryDetectVersion").invoke(null);
        method(type("net.minecraft.server.Bootstrap"), "bootStrap").invoke(null);
        try {
            Class<?> builtins = type("net.minecraft.core.registries.BuiltInRegistries");
            Object initializers = field(builtins, "DATA_COMPONENT_INITIALIZERS").get(null);
            Class<?> vanilla = Class.forName("net.minecraft.data.registries.VanillaRegistries");
            Object lookup;
            try {
                lookup = vanilla.getMethod("createWorldLookup").invoke(null);
                lookup = vanilla.getMethod("createReloadableLookup", Class.forName("net.minecraft.core.HolderLookup$Provider"))
                        .invoke(null, lookup);
            } catch (NoSuchMethodException e) {
                lookup = vanilla.getMethod("createLookup").invoke(null);
            }
            List<?> components = (List<?>) initializers.getClass().getMethod("build",
                    Class.forName("net.minecraft.core.HolderLookup$Provider")).invoke(initializers, lookup);
            Method apply = Class.forName("net.minecraft.core.component.DataComponentInitializers$PendingComponents").getMethod("apply");
            for (Object component : components) {
                apply.invoke(component);
            }
        } catch (NoSuchFieldException e) {
            // Pre-26.2 built-in items bind their own default components during bootstrap.
        }
        if (config != null) {
            initializeParts(config, new ArrayList<>());
            try {
                Object anticheat = config.getClass().getField("anticheat").get(config);
                Object obfuscation = anticheat.getClass().getField("obfuscation").get(anticheat);
                Object items = obfuscation.getClass().getField("items").get(obfuscation);
                metadataCodecUnavailable = items.getClass().getField("binding").get(items) == null;
            } catch (NoSuchFieldException e) {
                // Older Paper has no running-server item sanitizer dependency.
            }
        }

        Object rootRegistry = field(type("net.minecraft.core.registries.BuiltInRegistries"), "REGISTRY").get(null);
        registryAccess = method(type("net.minecraft.core.RegistryAccess"),
                "fromRegistryOfRegistries", type("net.minecraft.core.Registry")).invoke(null, rootRegistry);
        itemMetadataCodecUnavailable = args.length > 2 && args[2].equals("spigot");

        Class<?> legacyNBT = Class.forName("me.filoghost.holographicdisplays.nms.mapped.LegacyItemNBT");
        Method decodeNBT = legacyNBT.getDeclaredMethod("decode", String.class, String.class, int.class, Object.class);
        decodeNBT.setAccessible(true);
        int dataVersion = (Integer) Bukkit.getUnsafe().getClass().getMethod("getDataVersion").invoke(Bukkit.getUnsafe());
        Object converted = decodeNBT.invoke(null, "minecraft:diamond_sword",
                "{Damage:7,display:{Name:'{\"text\":\"Compatibility probe\"}'}}", dataVersion, registryAccess);
        Object ops = field(type("net.minecraft.nbt.NbtOps"), "INSTANCE").get(null);
        Class<?> opsType = type("com.mojang.serialization.DynamicOps");
        Object registryOps = method(type("net.minecraft.resources.RegistryOps"), "create",
                opsType, type("net.minecraft.core.HolderLookup$Provider")).invoke(null, ops, registryAccess);
        Object codec = field(type("net.minecraft.world.item.ItemStack"), "CODEC").get(null);
        Object encodedNBT = type("com.mojang.serialization.Encoder").getMethod("encodeStart", opsType, Object.class)
                .invoke(codec, registryOps, converted);
        Object tag = ((java.util.Optional<?>) type("com.mojang.serialization.DataResult").getMethod("result")
                .invoke(encodedNBT)).get();
        String serializedNBT = tag.toString();
        require(serializedNBT.contains("minecraft:custom_name") && serializedNBT.contains("Compatibility probe")
                && serializedNBT.contains("minecraft:damage") && serializedNBT.contains("7"), "Legacy icon NBT conversion");

        Method factory = null;
        for (Method candidate : selectedVersion.getClass().getMethods()) {
            if (candidate.getName().equals("createNMSManager") && candidate.getParameterCount() == 1) {
                factory = candidate;
                break;
            }
        }
        require(factory != null, "Adapter factory exists in the built plugin JAR");
        VersionNMSManager manager = (VersionNMSManager) factory.invoke(selectedVersion, (Object) null);
        PositionCoordinates original = new PositionCoordinates(12.25, 80.5, -7.75);
        PositionCoordinates moved = new PositionCoordinates(-11.75, 65.125, 9.5);
        TextNMSPacketEntity text = manager.newTextPacketEntity();
        List<Object> textSpawn = packets(text.newSpawnPackets(original, "§aHello §lworld"));
        require(textSpawn.size() == 2, "Text spawn packet count");
        require(number(textSpawn.get(0), "getY") == original.getY() - 0.29, "Text offset");
        check(textSpawn);
        check(packets(text.newChangePackets("")));
        check(packets(text.newChangePackets("§x§1§2§3§4§5§6RGB\nsecond line")));
        check(packets(text.newChangePackets(new String(new char[5100]).replace('\0', 'a'))));
        check(packets(text.newTeleportPackets(moved)));
        check(packets(text.newDestroyPackets()));

        ItemNMSPacketEntity item = manager.newItemPacketEntity();
        List<Object> itemSpawn = packets(item.newSpawnPackets(original, new ItemStack(Material.DIAMOND)));
        require(itemSpawn.size() == 5, "Item spawn packet count and mount");
        require(number(itemSpawn.get(0), "getY") == original.getY(), "Item vehicle offset");
        require(number(itemSpawn.get(2), "getData") == 1, "Item spawn object data");
        check(itemSpawn);
        check(packets(item.newChangePackets(new ItemStack(Material.STONE, 2))));
        check(packets(item.newTeleportPackets(moved)));
        check(packets(item.newDestroyPackets()));

        ClickableNMSPacketEntity clickable = manager.newClickablePacketEntity();
        List<Object> slimeSpawn = packets(clickable.newSpawnPackets(original));
        require(slimeSpawn.size() == 2, "Slime spawn packet count");
        check(slimeSpawn);
        check(packets(clickable.newTeleportPackets(moved)));
        check(packets(clickable.newDestroyPackets()));

        Class<?> accessClass = Class.forName("me.filoghost.holographicdisplays.nms.mapped.MappedAccess");
        Field singleton = accessClass.getDeclaredField("INSTANCE");
        singleton.setAccessible(true);
        Method interact = accessClass.getDeclaredMethod("interactedEntity", Object.class);
        interact.setAccessible(true);
        try {
            Object attack = Class.forName("net.minecraft.network.protocol.game.ServerboundAttackPacket")
                    .getConstructor(int.class).newInstance(clickable.getID().getNumericID());
            require(interact.invoke(singleton.get(null), attack).equals(clickable.getID().getNumericID()), "Attack entity ID");
        } catch (ClassNotFoundException e) {
            // Older versions use the single interact packet; field resolution was checked by initialization.
        }
        String result = "PASS " + args[0] + ": " + checked + " native packet codec round trips; offsets, order, item data and IDs";
        try (java.io.PrintWriter out = new java.io.PrintWriter("probe-result.txt")) {
            out.println(result);
            out.println("Actual server Bukkit version selected MAPPED: " + bukkitVersion);
            out.println("Legacy icon NBT name and damage conversion passed native component codec validation.");
            if (metadataCodecUnavailable) {
                out.println("Metadata packets constructed; metadata codec requires a running Paper server's item sanitizer and was not tested.");
            }
            if (itemMetadataCodecUnavailable) {
                out.println("Item metadata constructed; its codec requires loaded Bukkit world registries and was not tested.");
            }
        }
        System.out.println(result);
    }

    private static void initializeParts(Object part, List<Object> parents) throws Exception {
        List<Object> ancestors = new ArrayList<>(parents);
        ancestors.add(part);
        for (Field field : part.getClass().getFields()) {
            if (field.getType().getName().startsWith("io.papermc.paper.configuration.GlobalConfiguration$")
                    && !field.getType().isEnum() && field.get(part) == null) {
                Object child = null;
                for (java.lang.reflect.Constructor<?> constructor : field.getType().getConstructors()) {
                    if (constructor.getParameterCount() == 0) {
                        child = constructor.newInstance();
                        break;
                    }
                    if (constructor.getParameterCount() == 1) {
                        for (Object ancestor : ancestors) {
                            if (constructor.getParameterTypes()[0].isInstance(ancestor)) {
                                child = constructor.newInstance(ancestor);
                                break;
                            }
                        }
                    }
                }
                if (child != null) {
                    field.set(part, child);
                    initializeParts(child, ancestors);
                }
            }
        }
    }

    private static List<Object> packets(PacketGroup group) throws Exception {
        List<Object> result = new ArrayList<>();
        collect(group, result);
        return result;
    }

    private static void collect(Object group, List<Object> result) throws Exception {
        for (Class<?> type = group.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getName().equals("rawPacket")) {
                    field.setAccessible(true);
                    result.add(field.get(group));
                } else if (PacketGroup.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    collect(field.get(group), result);
                }
            }
        }
    }

    private static void check(List<Object> packets) throws Exception {
        for (Object packet : packets) {
            if (metadataCodecUnavailable && type("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket").isInstance(packet)) {
                continue;
            }
            if (itemMetadataCodecUnavailable && type("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket").isInstance(packet)) {
                List<?> values = (List<?>) method(packet.getClass(), "packedItems").invoke(packet);
                if (values.size() == 1 && type("net.minecraft.world.item.ItemStack").isInstance(
                        method(values.get(0).getClass(), "value").invoke(values.get(0)))) {
                    continue;
                }
            }
            Object codec = field(packet.getClass(), "STREAM_CODEC").get(null);
            Method encode = method(type("net.minecraft.network.codec.StreamEncoder"), "encode", Object.class, Object.class);
            Method decode = method(type("net.minecraft.network.codec.StreamDecoder"), "decode", Object.class);
            ByteBuf bytes = Unpooled.buffer();
            ByteBuf encoded = Unpooled.buffer();
            try {
                Object buffer = registryBuffer(bytes);
                encode.invoke(codec, buffer, packet);
                Object decoded = decode.invoke(codec, buffer);
                require(!bytes.isReadable(), "All packet bytes consumed: " + packet.getClass());
                encode.invoke(codec, registryBuffer(encoded), decoded);
                bytes.readerIndex(0);
                require(bytes.equals(encoded), "Packet codec round trip: " + packet.getClass());
                checked++;
            } finally {
                bytes.release();
                encoded.release();
            }
        }
    }

    private static Object registryBuffer(ByteBuf bytes) throws Exception {
        return type("net.minecraft.network.RegistryFriendlyByteBuf").getConstructor(
                ByteBuf.class, type("net.minecraft.core.RegistryAccess")).newInstance(bytes, registryAccess);
    }

    private static double number(Object target, String getter) throws Exception {
        return ((Number) method(target.getClass(), getter).invoke(target)).doubleValue();
    }

    private static Class<?> type(String name) throws Exception {
        Class<?> names = Class.forName("me.filoghost.holographicdisplays.nms.mapped.MappedNames");
        Method method = names.getDeclaredMethod("type", String.class);
        method.setAccessible(true);
        try {
            return (Class<?>) method.invoke(null, name);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Class<?> names = Class.forName("me.filoghost.holographicdisplays.nms.mapped.MappedNames");
        Method method = names.getDeclaredMethod("field", Class.class, String.class);
        method.setAccessible(true);
        try {
            return (Field) method.invoke(null, type, name);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... arguments) throws Exception {
        Class<?> names = Class.forName("me.filoghost.holographicdisplays.nms.mapped.MappedNames");
        Method method = names.getDeclaredMethod("method", Class.class, String.class, Class[].class);
        method.setAccessible(true);
        try {
            return (Method) method.invoke(null, type, name, arguments);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

}
