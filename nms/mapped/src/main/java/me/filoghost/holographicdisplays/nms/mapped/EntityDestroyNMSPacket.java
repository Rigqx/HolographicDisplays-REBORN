/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import me.filoghost.holographicdisplays.nms.common.EntityID;

class EntityDestroyNMSPacket extends VersionNMSPacket {

    EntityDestroyNMSPacket(EntityID id) {
        super(MappedAccess.INSTANCE.destroy(new int[] {id.getNumericID()}));
    }

    EntityDestroyNMSPacket(EntityID id1, EntityID id2) {
        super(MappedAccess.INSTANCE.destroy(new int[] {id1.getNumericID(), id2.getNumericID()}));
    }

}
