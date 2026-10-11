/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.filoghost.fcommons.logging.Log;
import me.filoghost.holographicdisplays.nms.common.NMSErrors;
import me.filoghost.holographicdisplays.nms.common.PacketListener;
import org.bukkit.entity.Player;

class InboundPacketHandler extends ChannelInboundHandlerAdapter {

    static final String HANDLER_NAME = "holographic_displays_listener";

    private final Player player;
    private final PacketListener listener;

    InboundPacketHandler(Player player, PacketListener listener) {
        this.player = player;
        this.listener = listener;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object packet) throws Exception {
        try {
            Integer id = MappedAccess.INSTANCE.interactedEntity(packet);
            if (id != null && listener.onAsyncEntityInteract(player, id)) {
                return;
            }
        } catch (Throwable t) {
            Log.warning(NMSErrors.EXCEPTION_ON_PACKET_READ, t);
        }
        super.channelRead(context, packet);
    }

}
