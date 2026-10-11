/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import me.filoghost.holographicdisplays.nms.common.EntityID;

class EntityMetadataNMSPacket extends VersionNMSPacket {

    EntityMetadataNMSPacket(Object rawPacket) {
        super(rawPacket);
    }

    static DataWatcherPacketBuilder<EntityMetadataNMSPacket> builder(EntityID id) {
        return new DataWatcherPacketBuilder<>(id);
    }

}
