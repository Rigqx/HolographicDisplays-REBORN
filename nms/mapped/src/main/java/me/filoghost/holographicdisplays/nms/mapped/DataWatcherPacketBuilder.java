/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import me.filoghost.fcommons.Strings;
import me.filoghost.holographicdisplays.nms.common.EntityID;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class DataWatcherPacketBuilder<T> {

    private static final int MAX_CUSTOM_NAME_LENGTH = 5000;

    private final EntityID id;
    private final List<Object> values = new ArrayList<>();

    DataWatcherPacketBuilder(EntityID id) {
        this.id = id;
    }

    DataWatcherPacketBuilder<T> setInvisible() {
        values.add(MappedAccess.INSTANCE.value(MappedAccess.INSTANCE.sharedFlags, (byte) 0x20));
        return this;
    }

    DataWatcherPacketBuilder<T> setArmorStandMarker() {
        setInvisible();
        // Same flags as the legacy adapter: small, show arms, no base plate, marker.
        values.add(MappedAccess.INSTANCE.value(MappedAccess.INSTANCE.armorStandFlags, (byte) (0x01 | 0x02 | 0x08 | 0x10)));
        return this;
    }

    DataWatcherPacketBuilder<T> setCustomName(String text) {
        text = Strings.truncate(text, MAX_CUSTOM_NAME_LENGTH);
        values.add(MappedAccess.INSTANCE.value(MappedAccess.INSTANCE.customName,
                Strings.isEmpty(text) ? Optional.empty() : Optional.of(MappedAccess.INSTANCE.chat(text))));
        values.add(MappedAccess.INSTANCE.value(MappedAccess.INSTANCE.nameVisible, !Strings.isEmpty(text)));
        return this;
    }

    DataWatcherPacketBuilder<T> setItemStack(ItemStack stack) {
        values.add(MappedAccess.INSTANCE.value(MappedAccess.INSTANCE.itemStack, MappedAccess.INSTANCE.item(stack)));
        return this;
    }

    DataWatcherPacketBuilder<T> setSlimeSmall() {
        values.add(MappedAccess.INSTANCE.value(MappedAccess.INSTANCE.slimeSize, 1));
        return this;
    }

    EntityMetadataNMSPacket build() {
        return new EntityMetadataNMSPacket(MappedAccess.INSTANCE.metadata(id, values));
    }

}
