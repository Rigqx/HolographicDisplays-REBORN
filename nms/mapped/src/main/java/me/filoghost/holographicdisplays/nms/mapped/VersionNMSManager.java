/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.nms.mapped;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import me.filoghost.fcommons.logging.ErrorCollector;
import me.filoghost.fcommons.logging.Log;
import me.filoghost.holographicdisplays.nms.common.NMSErrors;
import me.filoghost.holographicdisplays.nms.common.NMSManager;
import me.filoghost.holographicdisplays.nms.common.PacketListener;
import me.filoghost.holographicdisplays.nms.common.entity.ClickableNMSPacketEntity;
import me.filoghost.holographicdisplays.nms.common.entity.ItemNMSPacketEntity;
import me.filoghost.holographicdisplays.nms.common.entity.TextNMSPacketEntity;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class VersionNMSManager implements NMSManager {

    private final MappedAccess access;

    public VersionNMSManager(ErrorCollector errorCollector) {
        access = MappedAccess.INSTANCE;
    }

    @Override
    public TextNMSPacketEntity newTextPacketEntity() {
        return new VersionTextNMSPacketEntity(access.newEntityID());
    }

    @Override
    public ItemNMSPacketEntity newItemPacketEntity() {
        return new VersionItemNMSPacketEntity(access.newEntityID(), access.newEntityID());
    }

    @Override
    public ClickableNMSPacketEntity newClickablePacketEntity() {
        return new VersionClickableNMSPacketEntity(access.newEntityID());
    }

    @Override
    public void injectPacketListener(Player player, PacketListener listener) {
        modifyPipeline(player, pipeline -> {
            removeListener(pipeline);
            pipeline.addBefore("packet_handler", InboundPacketHandler.HANDLER_NAME, new InboundPacketHandler(player, listener));
        });
    }

    @Override
    public void uninjectPacketListener(Player player) {
        modifyPipeline(player, VersionNMSManager::removeListener);
    }

    private static void removeListener(ChannelPipeline pipeline) {
        if (pipeline.get(InboundPacketHandler.HANDLER_NAME) != null) {
            pipeline.remove(InboundPacketHandler.HANDLER_NAME);
        }
    }

    private void modifyPipeline(Player player, Consumer<ChannelPipeline> modifier) {
        try {
            Channel channel = access.channel(player);
            if (channel == null) {
                return;
            }
            Runnable task = () -> {
                if (!player.isOnline()) {
                    return;
                }
                try {
                    modifier.accept(channel.pipeline());
                } catch (Exception e) {
                    Log.warning(NMSErrors.EXCEPTION_MODIFYING_CHANNEL_PIPELINE, e);
                }
            };
            if (channel.eventLoop().inEventLoop()) {
                task.run();
            } else {
                channel.eventLoop().execute(task);
            }
        } catch (ReflectiveOperationException e) {
            Log.warning(NMSErrors.EXCEPTION_MODIFYING_CHANNEL_PIPELINE, e);
        }
    }

}
