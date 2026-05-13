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
                sender.sendMessage("Firestorm is ACTIVE (" + manager.getRemainingTicks() + " ticks remaining).");
            } else {
                sender.sendMessage("No firestorm is active.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            if (args.length >= 2) {
                int seconds;
                try {
                    seconds = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Usage: /firestorm start [seconds]");
                    return true;
                }
                manager.startStorm(seconds * 20);
                sender.sendMessage("Firestorm started for " + seconds + " seconds.");
            } else {
                manager.startStorm();
                sender.sendMessage("Firestorm started.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            if (!manager.isActive()) {
                sender.sendMessage("No firestorm is currently active.");
            } else {
                manager.stopStorm();
                sender.sendMessage("Firestorm stopped.");
            }
            return true;
        }

        sender.sendMessage("Usage: /firestorm [start [seconds] | stop | status]");
        return true;
    }
}