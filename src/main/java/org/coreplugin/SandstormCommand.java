package org.coreplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SandstormCommand implements CommandExecutor {

    private final SandstormManager manager;

    public SandstormCommand(SandstormManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            if (manager.isActive()) {
                sender.sendMessage("Solar Flare is ACTIVE (" + manager.getRemainingTicks() + " ticks remaining).");
            } else {
                sender.sendMessage("No Solar Flare is active.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            if (args.length >= 2) {
                int seconds;
                try {
                    seconds = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Usage: /solarflare start [seconds]");
                    return true;
                }
                manager.startStorm(seconds * 20);
                sender.sendMessage("Solar Flare started for " + seconds + " seconds.");
            } else {
                manager.startStorm();
                sender.sendMessage("Solar Flare started.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            if (!manager.isActive()) {
                sender.sendMessage("No Solar Flare is currently active.");
            } else {
                manager.stopStorm();
                sender.sendMessage("Solar Flare stopped.");
            }
            return true;
        }

        sender.sendMessage("Usage: /solarflare [start [seconds] | stop | status]");
        return true;
    }
}