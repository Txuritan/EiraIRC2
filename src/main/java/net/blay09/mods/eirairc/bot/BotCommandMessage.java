// Copyright (c) 2015, Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.bot;

import net.blay09.mods.eirairc.api.bot.IBotCommand;
import net.blay09.mods.eirairc.api.bot.IRCBot;
import net.blay09.mods.eirairc.api.irc.IRCChannel;
import net.blay09.mods.eirairc.api.irc.IRCUser;
import net.blay09.mods.eirairc.config.settings.BotSettings;
import net.blay09.mods.eirairc.net.NetworkHandler;
import net.blay09.mods.eirairc.net.message.MessageNotification;
import net.blay09.mods.eirairc.util.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class BotCommandMessage implements IBotCommand {

    @Override
    public String getCommandName() {
        return "msg";
    }

    @Override
    public boolean isChannelCommand() {
        return false;
    }

    @Override
    public void processCommand(IRCBot bot, IRCChannel channel, IRCUser user, String[] args, IBotCommand commandSettings) {
        BotSettings botSettings = ConfigHelper.getBotSettings(channel);
        if (!botSettings.allowPrivateMessages.get()) {
            user.notice(I19n.format("eirairc:commands.msg.disabled"));
        }
        String playerName = args[0];
        EntityPlayer entityPlayer = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(playerName);
        if (entityPlayer == null) {
            PlayerList playerEntityList = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList();
            for (EntityPlayer entity : playerEntityList.getPlayers()) {
                if (Utils.getNickGame(entity).equals(playerName) || Utils.getNickIRC(entity, channel).equals(playerName)) {
                    entityPlayer = entity;
                }
            }
            if (entityPlayer == null) {
                user.notice(I19n.format("eirairc:general.noSuchPlayer"));
                return;
            }
        }
        String message = StringUtils.join(args, " ", 1, args.length);
        if (botSettings.filterLinks.get()) {
            message = MessageFormat.filterLinks(message);
        }
        ITextComponent chatComponent = MessageFormat.formatChatComponent(botSettings.getMessageFormat().mcPrivateMessage, bot.getConnection(), null, user, message, MessageFormat.Target.Minecraft, MessageFormat.Mode.Message);
        String notifyMsg = chatComponent.getUnformattedText();
        if (notifyMsg.length() > 42) {
            notifyMsg = notifyMsg.substring(0, 42) + "...";
        }
        NetworkHandler.instance.sendTo(new MessageNotification(NotificationType.PrivateMessage, notifyMsg), ((EntityPlayerMP) entityPlayer));
        entityPlayer.sendMessage(chatComponent);
        user.notice(I19n.format("eirairc:bot.msgSent", playerName, message));
    }

    @Override
    public boolean requiresAuth() {
        return false;
    }

    @Override
    public boolean broadcastsResult() {
        return false;
    }

    @Override
    public boolean allowArgs() {
        return true;
    }

    @Override
    public String getCommandDescription() {
        return "Send a private message to an online player.";
    }

}
