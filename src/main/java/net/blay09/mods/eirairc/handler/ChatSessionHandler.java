// Copyright (c) 2015, Christopher "BlayTheNinth" Baker


package net.blay09.mods.eirairc.handler;

import net.blay09.mods.eirairc.api.irc.IRCChannel;
import net.blay09.mods.eirairc.api.irc.IRCContext;
import net.blay09.mods.eirairc.api.irc.IRCUser;
import net.blay09.mods.eirairc.util.ConfigHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatSessionHandler {

    private IRCContext chatTarget = null;
    private final List<IRCChannel> validTargetChannels = new ArrayList<>();
    private final List<IRCUser> validTargetUsers = new ArrayList<>();
    private int targetChannelIdx = -1;
    private int targetUserIdx = 0;

    public void addTargetUser(IRCUser user) {
        if (!validTargetUsers.contains(user)) {
            validTargetUsers.add(user);
        }
    }

    public void addTargetChannel(IRCChannel channel) {
        if (!validTargetChannels.contains(channel)) {
            validTargetChannels.add(channel);
        }
    }

    public void removeTargetUser(IRCUser user) {
        validTargetUsers.remove(user);
    }

    public void removeTargetChannel(IRCChannel channel) {
        validTargetChannels.remove(channel);
    }

    public void setChatTarget(IRCContext chatTarget) {
        this.chatTarget = chatTarget;
        if (chatTarget == null) {
            targetChannelIdx = -1;
            targetUserIdx = 0;
        } else if (chatTarget instanceof IRCChannel) {
            targetUserIdx = 0;
            targetChannelIdx = validTargetChannels.indexOf(chatTarget);
        } else if (chatTarget instanceof IRCUser) {
            targetChannelIdx = -1;
            targetUserIdx = validTargetUsers.indexOf(chatTarget);
        }
    }
    
    public IRCContext getChatTarget() {
        return chatTarget;
    }

    public IRCContext getNextTarget(boolean users) {
        if (users) {
            if (validTargetUsers.isEmpty()) {
                return null;
            }
            targetUserIdx++;
            if (targetUserIdx >= validTargetUsers.size()) {
                targetUserIdx = 0;
            }
            return validTargetUsers.get(targetUserIdx);
        } else {
            if (validTargetChannels.isEmpty()) {
                return null;
            }
            targetChannelIdx++;
            if (targetChannelIdx >= validTargetChannels.size()) {
                targetChannelIdx = -1;
            }
            if (targetChannelIdx == -1) {
                return null;
            }
            IRCChannel channel = validTargetChannels.get(targetChannelIdx);
            if (ConfigHelper.getChannelConfig(channel).getGeneralSettings().readOnly.get()) {
                return getNextTarget(false);
            }
            return channel;
        }
    }

    public void clear() {
        chatTarget = null;
        validTargetChannels.clear();
        validTargetUsers.clear();
        targetChannelIdx = -1;
        targetUserIdx = 0;
    }
}
