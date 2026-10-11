/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

final class MappedNames {

    private static final Properties NAMES = load();

    private MappedNames() {}

    private static Properties load() {
        Properties names = new Properties();
        String serverPackage = Bukkit.getServer().getClass().getPackage().getName();
        if (serverPackage.equals("org.bukkit.craftbukkit")) {
            return names;
        }
        String version = Bukkit.getBukkitVersion().split("-R", 2)[0];
        try (InputStream input = MappedNames.class.getResourceAsStream("/holographicdisplays/nms/spigot-" + version + ".properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing Spigot mappings for " + version);
            }
            names.load(input);
            if (!serverPackage.equals(names.getProperty("serverPackage"))) {
                throw new IllegalStateException("Unexpected Spigot package for " + version + ": " + serverPackage);
            }
            return names;
        } catch (IOException e) {
            throw new IllegalStateException("Could not load Spigot mappings for " + version, e);
        }
    }

    static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(NAMES.getProperty("class." + name, name), false, MappedNames.class.getClassLoader());
    }

    static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(NAMES.getProperty("field." + type.getName() + "." + name, name));
        field.setAccessible(true);
        return field;
    }

    static Method method(Class<?> type, String name, Class<?>... parameters) throws NoSuchMethodException {
        StringBuilder key = new StringBuilder("method.").append(type.getName()).append('.').append(name).append('(');
        for (Class<?> parameter : parameters) {
            key.append(parameter.getTypeName()).append(',');
        }
        key.append(')');
        return type.getMethod(NAMES.getProperty(key.toString(), name), parameters);
    }

}
