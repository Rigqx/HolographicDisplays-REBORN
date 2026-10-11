/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import me.filoghost.holographicdisplays.common.PositionCoordinates;
import me.filoghost.holographicdisplays.nms.common.EntityID;

class EntitySpawnNMSPacket extends VersionNMSPacket {

    EntitySpawnNMSPacket(EntityID id, Object type, PositionCoordinates position, double offsetY) {
        super(MappedAccess.INSTANCE.spawn(id, type, position, offsetY));
    }

}
