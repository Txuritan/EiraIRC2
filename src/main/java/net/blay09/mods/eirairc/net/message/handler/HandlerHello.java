// Copyright (c) 2015, Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.net.message.handler;

import net.blay09.mods.eirairc.EiraIRC;
import net.blay09.mods.eirairc.net.EiraPlayerInfo;
import net.blay09.mods.eirairc.net.message.MessageHello;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class HandlerHello implements IMessageHandler<MessageHello, IMessage> {

    @Override
    public IMessage onMessage(MessageHello packet, MessageContext ctx) {
        EiraIRC.proxy.addScheduledTask(() -> {
            EntityPlayer entityPlayer = ctx.getServerHandler().player;

            EiraPlayerInfo playerInfo = EiraIRC.instance.getNetHandler().getPlayerInfo(entityPlayer.getName());
            playerInfo.modInstalled = true;
            playerInfo.modVersion = packet.getVersion();
        });
        return null;
    }

}
