/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;

/** Converts the existing icon NBT syntax to native item components without changing stored configuration. */
public final class LegacyItemNBT {

    // Last pre-component item format, Minecraft 1.20.4.
    private static final int LEGACY_DATA_VERSION = 3700;

    private LegacyItemNBT() {}

    public static void apply(ItemStack itemStack, String nbt) throws ReflectiveOperationException {
        String materialKey = itemStack.getType().getClass().getMethod("getKey").invoke(itemStack.getType()).toString();
        int currentVersion = (Integer) Bukkit.getUnsafe().getClass().getMethod("getDataVersion").invoke(Bukkit.getUnsafe());
        String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
        Object registries = type(craftPackage + ".CraftRegistry").getMethod("getMinecraftRegistry").invoke(null);
        Object decoded = decode(materialKey, nbt, currentVersion, registries);
        Class<?> craftItem = type(craftPackage + ".inventory.CraftItemStack");
        for (Method method : craftItem.getMethods()) {
            if (method.getName().equals("asBukkitCopy") && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isInstance(decoded)) {
                ItemStack converted = (ItemStack) method.invoke(null, decoded);
                itemStack.setItemMeta(converted.getItemMeta());
                return;
            }
        }
        throw new NoSuchMethodException("CraftItemStack.asBukkitCopy");
    }

    static Object decode(String materialKey, String nbt, int currentVersion, Object registries)
            throws ReflectiveOperationException {
        Object tag = parse("{id:\"" + materialKey + "\",Count:1b,tag:" + nbt + "}");
        Object nbtOps = MappedNames.field(type("net.minecraft.nbt.NbtOps"), "INSTANCE").get(null);
        Class<?> opsType = type("com.mojang.serialization.DynamicOps");
        Class<?> dynamicType = type("com.mojang.serialization.Dynamic");
        Object dynamic = dynamicType.getConstructor(opsType, Object.class).newInstance(nbtOps, tag);
        Object fixer = MappedNames.method(type("net.minecraft.util.datafix.DataFixers"), "getDataFixer").invoke(null);
        Object reference = MappedNames.field(type("net.minecraft.util.datafix.fixes.References"), "ITEM_STACK").get(null);
        Object updated = type("com.mojang.datafixers.DataFixer").getMethod("update",
                type("com.mojang.datafixers.DSL$TypeReference"), dynamicType, int.class, int.class)
                .invoke(fixer, reference, dynamic, LEGACY_DATA_VERSION, currentVersion);
        Object registryOps = MappedNames.method(type("net.minecraft.resources.RegistryOps"),
                "create", opsType, type("net.minecraft.core.HolderLookup$Provider")).invoke(null, nbtOps, registries);
        Class<?> nmsItem = type("net.minecraft.world.item.ItemStack");
        Object codec = MappedNames.field(nmsItem, "CODEC").get(null);
        Object result = type("com.mojang.serialization.Codec").getMethod("parse", opsType, Object.class)
                .invoke(codec, registryOps, dynamicType.getMethod("getValue").invoke(updated));
        Optional<?> decoded = (Optional<?>) type("com.mojang.serialization.DataResult").getMethod("result").invoke(result);
        if (!decoded.isPresent()) {
            throw new IllegalArgumentException("Could not convert legacy icon NBT to the server's item components: " + result);
        }
        return decoded.get();
    }

    private static Object parse(String input) throws ReflectiveOperationException {
        Class<?> parser = type("net.minecraft.nbt.TagParser");
        Method parse;
        try {
            parse = MappedNames.method(parser, "parseCompoundFully", String.class);
        } catch (NoSuchMethodException e) {
            parse = MappedNames.method(parser, "parseTag", String.class);
        }
        return parse.invoke(null, input);
    }

    private static Class<?> type(String name) throws ClassNotFoundException {
        return MappedNames.type(name);
    }

}
