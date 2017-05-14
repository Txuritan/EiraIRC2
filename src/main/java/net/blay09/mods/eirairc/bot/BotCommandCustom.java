// Copyright (c) 2015, Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.bot;

import com.google.gson.JsonObject;
import net.blay09.mods.eirairc.api.bot.IBotCommand;
import net.blay09.mods.eirairc.api.bot.IRCBot;
import net.blay09.mods.eirairc.api.irc.IRCChannel;
import net.blay09.mods.eirairc.api.irc.IRCUser;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.StringUtils;

public class BotCommandCustom implements IBotCommand {

    private String name = "";
    private String command = "";
    private String description;
    private boolean allowArgs;
    private boolean runAsOp;
    private boolean requireAuth;
    private boolean broadcastResult;
    private String outputFilter;
    private IBotCommand overrideCommand;

    @Override
    public String getCommandName() {
        return name;
    }

    @Override
    public boolean isChannelCommand() {
        return true;
    }

    @Override
    public void processCommand(IRCBot bot, IRCChannel channel, IRCUser user, String[] args, IBotCommand commandSettings) {
        if (overrideCommand != null) {
            overrideCommand.processCommand(bot, channel, user, args, this);
        } else {
            String message = command;
            if (commandSettings.allowArgs()) {
                message += " " + StringUtils.join(args, " ", 0, args.length).trim();
            }
            FMLCommonHandler.instance().getMinecraftServerInstance().getServer().getCommandManager().executeCommand(new IRCUserCommandSender(channel, user, commandSettings.broadcastsResult(), runAsOp, outputFilter), message);
        }
    }

    @Override
    public boolean requiresAuth() {
        return requireAuth;
    }

    @Override
    public boolean broadcastsResult() {
        return broadcastResult;
    }

    @Override
    public boolean allowArgs() {
        return allowArgs;
    }

    @Override
    public String getCommandDescription() {
        return description;
    }

    public static BotCommandCustom loadFromJson(JsonObject object) {
        BotCommandCustom cmd = new BotCommandCustom();
        cmd.name = object.get("name").getAsString();
        cmd.command = object.has("command") ? object.get("command").getAsString() : "";
        cmd.allowArgs = object.has("allowArgs") && object.get("allowArgs").getAsBoolean();
        cmd.requireAuth = object.has("requireAuth") && object.get("requireAuth").getAsBoolean();
        cmd.broadcastResult = object.has("broadcastResult") && object.get("broadcastResult").getAsBoolean();
        cmd.description = object.has("description") ? object.get("description").getAsString() : "(no description set)";
        cmd.runAsOp = object.has("runAsOp") && object.get("runAsOp").getAsBoolean();
        if (object.has("override")) {
            String overrideName = object.get("override").getAsString();
            switch (overrideName) {
                case "who":
                    cmd.overrideCommand = new BotCommandWho();
                    break;
                case "help":
                    cmd.overrideCommand = new BotCommandHelp();
                    break;
                case "msg":
                    cmd.overrideCommand = new BotCommandMessage();
                    break;
                case "op":
                    cmd.overrideCommand = new BotCommandOp();
                    break;
            }
        }
        cmd.outputFilter = object.has("outputFilter") ? object.get("outputFilter").getAsString() : "";
        return cmd;
    }
}
