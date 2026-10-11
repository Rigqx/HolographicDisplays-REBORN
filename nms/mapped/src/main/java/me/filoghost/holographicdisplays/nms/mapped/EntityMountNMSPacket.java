/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import me.filoghost.holographicdisplays.nms.common.EntityID;

class EntityMountNMSPacket extends VersionNMSPacket {

    EntityMountNMSPacket(EntityID vehicle, EntityID passenger) {
        super(MappedAccess.INSTANCE.mount(vehicle, passenger));
    }

}
