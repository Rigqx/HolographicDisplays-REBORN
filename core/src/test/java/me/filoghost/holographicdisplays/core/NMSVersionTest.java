/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.core;

import me.filoghost.holographicdisplays.core.test.Mocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NMSVersionTest {

    @BeforeAll
    static void prepareBukkit() {
        Mocks.prepareEnvironment();
    }

    @Test
    void legacyPackagesStillSelectTheirOriginalAdapter() {
        for (NMSVersion version : NMSVersion.values()) {
            if (version.name().startsWith("v")) {
                assertThat(NMSVersion.detectVersion("org.bukkit.craftbukkit." + version.name(), "26.3-R0.1-SNAPSHOT"))
                        .isSameAs(version);
            }
        }
    }

    @Test
    void mappedReleasesSelectTheIsolatedAdapter() {
        String[] releases = {"1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.3", "1.21.4", "1.21.5",
                "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2", "26.3"};
        for (String release : releases) {
            assertThat(NMSVersion.detectVersion("org.bukkit.craftbukkit", release + "-R0.1-SNAPSHOT"))
                    .isSameAs(NMSVersion.MAPPED);
        }
    }

    @Test
    void paperBuildVersionsSelectTheModernAdapter() {
        for (String release : new String[] {"26.1", "26.1.1", "26.1.2", "26.2", "26.3"}) {
            for (String channel : new String[] {"alpha", "beta", "stable"}) {
                assertThat(NMSVersion.detectVersion("org.bukkit.craftbukkit", release + ".build.133-" + channel))
                        .isSameAs(NMSVersion.MAPPED);
            }
        }
        for (String version : new String[] {"26.4.build.133-stable", "26.2.build.133-unknown",
                "26.2.build.133-experimental", "26.2-pre.1.build.133-stable", "26.2.build.invalid-stable",
                "26.2.build.133-stable-extra"}) {
            assertThat(NMSVersion.detectVersion("org.bukkit.craftbukkit", version)).isSameAs(NMSVersion.UNKNOWN);
        }
    }

    @Test
    void obfuscatedSpigotRequiresTheMatchingVersionProfile() throws Exception {
        String[] releases = {"1.20.5", "1.20.6", "1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.6",
                "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11"};
        for (String release : releases) {
            java.util.Properties mappings = new java.util.Properties();
            try (java.io.InputStream input = getClass().getResourceAsStream("/holographicdisplays/nms/spigot-" + release + ".properties")) {
                assertThat(input).isNotNull();
                mappings.load(input);
            }
            assertThat(NMSVersion.detectVersion(mappings.getProperty("serverPackage"), release + "-R0.1-SNAPSHOT"))
                    .isSameAs(NMSVersion.MAPPED);
            assertThat(NMSVersion.detectVersion(mappings.getProperty("serverPackage"), "27.1-R0.1-SNAPSHOT"))
                    .isSameAs(NMSVersion.UNKNOWN);
        }
    }

    @Test
    void unknownVersionsAndServerImplementationsAreRejected() {
        for (String release : new String[] {"1.20.4", "1.21.2", "26.3-pre-2", "26.4", "27.1", "invalid"}) {
            assertThat(NMSVersion.detectVersion("org.bukkit.craftbukkit", release + "-R0.1-SNAPSHOT"))
                    .isSameAs(NMSVersion.UNKNOWN);
        }
        assertThat(NMSVersion.detectVersion("org.bukkit.craftbukkit.v26_3_R1", "26.3-R0.1-SNAPSHOT"))
                .isSameAs(NMSVersion.UNKNOWN);
        assertThat(NMSVersion.detectVersion("com.example.server", "26.3-R0.1-SNAPSHOT"))
                .isSameAs(NMSVersion.UNKNOWN);
    }

    @Test
    void legacyOutdatedVersionErrorsRemainUnchanged() {
        assertThatThrownBy(() -> NMSVersion.v1_8_R1.createNMSManager(null))
                .isInstanceOf(NMSVersion.OutdatedVersionException.class)
                .extracting("minimumSupportedVersion").isEqualTo("1.8.4");
        assertThatThrownBy(() -> NMSVersion.UNKNOWN.createNMSManager(null))
                .isInstanceOf(NMSVersion.UnknownVersionException.class);
    }

}
