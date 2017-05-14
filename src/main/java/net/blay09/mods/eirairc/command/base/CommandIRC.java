// Copyright (c) 2015, Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.command.base;

import net.blay09.mods.eirairc.util.Globals;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;

public class CommandIRC implements ICommand {

    @Override
    public int compareTo(ICommand o) {
        return getName().compareTo((o.getName()));
    }

    @Override
    public String getName() {
        return "irc";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return Globals.MOD_ID + ":irc.commands.irc";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            IRCCommandHandler.sendUsageHelp(sender);
            return;
        }
        IRCCommandHandler.processCommand(sender, args, false);
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        return IRCCommandHandler.addTabCompletionOptions(sender, args, pos);
    }

    @Override
    public boolean isUsernameIndex(String[] sender, int args) {
        return IRCCommandHandler.isUsernameIndex(sender, args);
    }

}
