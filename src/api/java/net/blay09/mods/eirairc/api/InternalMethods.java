// Copyright (c) 2015 Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.api;

import net.blay09.mods.eirairc.api.config.IConfigManager;
import net.blay09.mods.eirairc.api.irc.IRCContext;
import net.blay09.mods.eirairc.api.upload.UploadHoster;
import net.minecraft.command.ICommandSender;

public interface InternalMethods {
    boolean isConnectedTo(String serverHost);

    IRCContext parseContext(IRCContext parentContext, String contextPath, IRCContext.ContextType expectedType);

    void registerSubCommand(SubCommand command);

    void registerUploadHoster(UploadHoster uploadHoster);

    boolean hasClientSideInstalled(ICommandSender user);

    void relayChat(ICommandSender sender, String message, boolean isEmote, boolean isNotice, IRCContext target);

    IConfigManager getSharedGlobalConfig();

    IConfigManager getClientGlobalConfig();
}
