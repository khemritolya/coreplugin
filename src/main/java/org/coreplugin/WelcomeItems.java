package org.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WelcomeItems {

    public static ItemStack loadFood(Plugin plugin, Player player) {
        ItemStack gloop = new ItemStack(Material.COOKIE, 5);
        ItemMeta meta = gloop.getItemMeta();
        meta.setLore(Collections.singletonList(ChatColor.RESET + "" + ChatColor.GRAY + "by Nitchisu Inc."));
        gloop.setItemMeta(meta);
        return gloop;
    }

    public static ItemStack loadBook(Plugin plugin, Player player) {
        File bookFile = new File(plugin.getDataFolder(), "welcome_book.txt");
        List<String> lines;
        try {
            lines = Files.readAllLines(bookFile.toPath());
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read welcome_book.txt: " + e.getMessage());
            return null;
        }

        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        for (String line : lines) {
            if (line.equals("\\NEWPAGE")) {
                pages.add(page.toString());
                page = new StringBuilder();
            } else {
                if (page.length() > 0) page.append('\n');
                page.append(applyColors(line, player, plugin));
            }
        }
        pages.add(page.toString());

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(ChatColor.RESET + player.getName() + "'s Data Pad");
        meta.setAuthor("New Fuji Co. Ltd.");
        meta.setLore(Collections.singletonList(ChatColor.DARK_GRAY + "Long Live the Emperor!"));
        meta.setPages(pages);
        book.setItemMeta(meta);
        return book;
    }

    private static String applyColors(String line, Player player, Plugin plugin) {
        line = line.replace("\\NAME", player.getName());
        line = line.replace("\\CRIME", JoinListener.getCrime(plugin, player).toUpperCase());
        for (ChatColor color : ChatColor.values()) {
            line = line.replace("\\" + color.name(), color.toString());
        }
        return line;
    }
}