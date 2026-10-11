/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import me.filoghost.holographicdisplays.nms.common.PacketGroup;
import org.bukkit.entity.Player;

class VersionNMSPacket implements PacketGroup {

    private final Object rawPacket;

    VersionNMSPacket(Object rawPacket) {
        this.rawPacket = rawPacket;
    }

    @Override
    public void sendTo(Player player) {
        MappedAccess.INSTANCE.send(player, rawPacket);
    }

}
