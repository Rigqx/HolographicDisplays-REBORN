/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
// Modified for HolographicDisplays-Reborn on 2026-10-11. See NOTICE.txt.
package me.filoghost.holographicdisplays.core;

import me.filoghost.fcommons.logging.ErrorCollector;
import me.filoghost.holographicdisplays.nms.common.NMSManager;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The package name used by version-dependent Bukkit classes and NMS classes (before 1.17), for example "v1_13_R2".
 * Different versions usually imply internal changes that require multiple implementations.
 */
public enum NMSVersion {

    // Not using shorter method reference syntax here because it initializes the class, causing a ClassNotFoundException

    /* 1.8 - 1.8.2     */ v1_8_R1(NMSManagerFactory.outdatedVersion("1.8.4")),
    /* 1.8.3           */ v1_8_R2(NMSManagerFactory.outdatedVersion("1.8.4")),
    /* 1.8.4 - 1.8.9   */ v1_8_R3(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_8_R3.VersionNMSManager(errorCollector)),
    /* 1.9 - 1.9.3     */ v1_9_R1(NMSManagerFactory.outdatedVersion("1.9.4")),
    /* 1.9.4           */ v1_9_R2(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_9_R2.VersionNMSManager(errorCollector)),
    /* 1.10 - 1.10.2   */ v1_10_R1(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_10_R1.VersionNMSManager(errorCollector)),
    /* 1.11 - 1.11.2   */ v1_11_R1(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_11_R1.VersionNMSManager(errorCollector)),
    /* 1.12 - 1.12.2   */ v1_12_R1(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_12_R1.VersionNMSManager(errorCollector)),
    /* 1.13            */ v1_13_R1(NMSManagerFactory.outdatedVersion("1.13.1")),
    /* 1.13.1 - 1.13.2 */ v1_13_R2(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_13_R2.VersionNMSManager(errorCollector)),
    /* 1.14 - 1.14.4   */ v1_14_R1(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_14_R1.VersionNMSManager(errorCollector)),
    /* 1.15 - 1.15.2   */ v1_15_R1(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_15_R1.VersionNMSManager(errorCollector)),
    /* 1.16 - 1.16.1   */ v1_16_R1(NMSManagerFactory.outdatedVersion("1.16.4")),
    /* 1.16.2 - 1.16.3 */ v1_16_R2(NMSManagerFactory.outdatedVersion("1.16.4")),
    /* 1.16.4 - 1.16.5 */ v1_16_R3(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_16_R3.VersionNMSManager(errorCollector)),
    /* 1.17            */ v1_17_R1(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_17_R1.VersionNMSManager(errorCollector)),
    /* 1.18 - 1.18.1   */ v1_18_R1(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_18_R1.VersionNMSManager(errorCollector)),
    /* 1.18.2          */ v1_18_R2(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_18_R2.VersionNMSManager(errorCollector)),
    /* 1.19 - 1.19.2   */ v1_19_R1(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_19_R1.VersionNMSManager(errorCollector)),
    /* 1.19.3          */ v1_19_R2(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_19_R2.VersionNMSManager(errorCollector)),
    /* 1.19.4          */ v1_19_R3(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_19_R3.VersionNMSManager(errorCollector)),
    /* 1.20 - 1.20.1   */ v1_20_R1(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_20_R1.VersionNMSManager(errorCollector)),
    /* 1.20.2          */ v1_20_R2(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_20_R2.VersionNMSManager(errorCollector)),
    /* 1.20.3 - 1.20.4 */ v1_20_R3(errorCollector -> new me.filoghost.holographicdisplays.nms.v1_20_R3.VersionNMSManager(errorCollector)),
    /* Mojang mappings */ MAPPED(errorCollector -> new me.filoghost.holographicdisplays.nms.mapped.VersionNMSManager(errorCollector)),
    /* Other versions  */ UNKNOWN(NMSManagerFactory.unknownVersion());

    private static final NMSVersion CURRENT_VERSION = detectCurrentVersion();

    private final NMSManagerFactory nmsManagerFactory;

    NMSVersion(NMSManagerFactory nmsManagerFactory) {
        this.nmsManagerFactory = nmsManagerFactory;
    }

    public NMSManager createNMSManager(ErrorCollector errorCollector) throws OutdatedVersionException, UnknownVersionException {
        return nmsManagerFactory.create(errorCollector);
    }

    public static NMSVersion getCurrent() {
        return CURRENT_VERSION;
    }

    private static NMSVersion detectCurrentVersion() {
        return detectVersion(Bukkit.getServer().getClass().getPackage().getName(), Bukkit.getBukkitVersion());
    }

    static NMSVersion detectVersion(String serverPackage, String bukkitVersion) {
        Matcher matcher = Pattern.compile("v\\d+_\\d+_R\\d+").matcher(serverPackage);
        if (!matcher.find()) {
            if (serverPackage.equals("org.bukkit.craftbukkit") && isMappedVersion(bukkitVersion)) {
                return MAPPED;
            }
            return UNKNOWN;
        }

        String nmsVersionName = matcher.group();
        try {
            return valueOf(nmsVersionName);
        } catch (IllegalArgumentException e) {
            if (isMappedVersion(bukkitVersion) && hasSpigotMappings(serverPackage, bukkitVersion)) {
                return MAPPED;
            }
            return UNKNOWN;
        }
    }

    private static boolean hasSpigotMappings(String serverPackage, String bukkitVersion) {
        String version = getMappedRelease(bukkitVersion);
        try (InputStream input = NMSVersion.class.getResourceAsStream("/holographicdisplays/nms/spigot-" + version + ".properties")) {
            if (input == null) {
                return false;
            }
            Properties mappings = new Properties();
            mappings.load(input);
            return serverPackage.equals(mappings.getProperty("serverPackage"));
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isMappedVersion(String bukkitVersion) {
        // An exact allowlist avoids silently treating future versions and snapshots as compatible.
        String version = getMappedRelease(bukkitVersion);
        switch (version) {
            case "1.20.5":
            case "1.20.6":
            case "1.21":
            case "1.21.1":
            case "1.21.3":
            case "1.21.4":
            case "1.21.5":
            case "1.21.6":
            case "1.21.7":
            case "1.21.8":
            case "1.21.9":
            case "1.21.10":
            case "1.21.11":
            case "26.1":
            case "26.1.1":
            case "26.1.2":
            case "26.2":
            case "26.3":
                return true;
            default:
                return false;
        }
    }

    private static String getMappedRelease(String bukkitVersion) {
        // Paper's 26-series API uses e.g. 26.2.build.133-stable instead of the Spigot -R suffix.
        Matcher paperVersion = Pattern.compile("^(\\d+\\.\\d+(?:\\.\\d+)?)\\.build\\.\\d+-(?:alpha|beta|stable)$").matcher(bukkitVersion);
        return paperVersion.matches() ? paperVersion.group(1) : bukkitVersion.split("-R", 2)[0];
    }


    @FunctionalInterface
    private interface NMSManagerFactory {

        NMSManager create(ErrorCollector errorCollector) throws UnknownVersionException, OutdatedVersionException;

        static NMSManagerFactory unknownVersion() {
            return errorCollector -> {
                throw new UnknownVersionException();
            };
        }

        static NMSManagerFactory outdatedVersion(String minimumSupportedVersion) {
            return errorCollector -> {
                throw new OutdatedVersionException(minimumSupportedVersion);
            };
        }

    }

    public static class UnknownVersionException extends Exception {}

    public static class OutdatedVersionException extends Exception {

        private final String minimumSupportedVersion;

        public OutdatedVersionException(String minimumSupportedVersion) {
            this.minimumSupportedVersion = minimumSupportedVersion;
        }

        public String getMinimumSupportedVersion() {
            return minimumSupportedVersion;
        }

    }

}
