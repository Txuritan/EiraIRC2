// Copyright (c) 2015, Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.command;

import com.google.common.collect.Lists;
import net.blay09.mods.eirairc.api.SubCommand;
import net.blay09.mods.eirairc.util.Utils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SubCommandWrapper implements ICommand {

    public final SubCommand command;

    public SubCommandWrapper(SubCommand command) {
        this.command = command;
    }

    @Override
    public String getName() {
        return command.getCommandName();
    }

    @Override
    public final String getUsage(ICommandSender sender) {
        return command.getCommandUsage(sender);
    }

    @Override
    public final List<String> getAliases() {
        String[] aliases = command.getAliases();
        if (aliases != null) {
            List<String> list = new ArrayList<>();
            Collections.addAll(list, aliases);
            return list;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return command.canCommandSenderUseCommand(sender);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int idx) {
        return command.isUsernameIndex(args, idx);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        command.processCommand(sender, Utils.getSuggestedTarget(), args, Utils.isServerSide());
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        List<String> list = Lists.newArrayList();
        command.addTabCompletionOptions(list, sender, args);
        return list;
    }

    @Override
    public int compareTo(ICommand o) {
        return getName().compareTo(o.getName());
    }

}
