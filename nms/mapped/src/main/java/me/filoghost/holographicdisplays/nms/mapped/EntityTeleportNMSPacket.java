/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import me.filoghost.holographicdisplays.common.PositionCoordinates;
import me.filoghost.holographicdisplays.nms.common.EntityID;

class EntityTeleportNMSPacket extends VersionNMSPacket {

    EntityTeleportNMSPacket(EntityID id, PositionCoordinates position, double offsetY) {
        super(MappedAccess.INSTANCE.teleport(id, position, offsetY));
    }

}
