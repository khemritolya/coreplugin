package org.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.coreplugin.RngUtils.poissonSample;

public class CustomItems {

    public static ItemStack loadSpice(Random rng, int mark, int duration) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        meta.setDisplayName(SilverfishSpawnListener.spiceName);

        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.SATURATION,     duration, mark,   false, false), true);
        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.REGENERATION,   duration, mark,   false, false), true);
        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, mark/2, false, false), true);
        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.NIGHT_VISION,   duration, 0,      false, false), true);

        meta.setLore(Collections.singletonList(ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Grade: " + ChatColor.GOLD + toRoman(mark + 1)));
        potion.setItemMeta(meta);
        return potion;
    }

    private static String toRoman(int n) {
        String[] M  = {"", "M", "MM", "MMM"};
        String[] C  = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] X  = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] I  = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return M[n / 1000] + C[(n % 1000) / 100] + X[(n % 100) / 10] + I[n % 10];
    }

    public static ItemStack loadCookies(int amount) {
        ItemStack gloop = new ItemStack(Material.COOKIE, amount);
        ItemMeta meta = gloop.getItemMeta();
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Nitchisu Inc.",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Imperial Quality™"));
        gloop.setItemMeta(meta);
        return gloop;
    }

    public static ItemStack loadMonomolecularBlade(int durability) {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.DARK_RED + "Monomolecular Blade");
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Amakuni Concern",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Arming the Empire™"));
        sword.setItemMeta(meta);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 10);
        sword.setDurability((short) (1561 - Math.min(durability, 1551)));
        return sword;
    }

    public static ItemStack loadWaterBucket() {
        return new ItemStack(Material.WATER_BUCKET);
    }

    public static ItemStack loadCowEgg() {
        ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1, EntityType.COW.getTypeId());
        ItemMeta meta = egg.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "Bioassembler");
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Nisso Biochem",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Spawns: " + ChatColor.DARK_BLUE + "Cow"));
        egg.setItemMeta(meta);
        return egg;
    }

    public static ItemStack loadPigEgg() {
        ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1, EntityType.PIG.getTypeId());
        ItemMeta meta = egg.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "Bioassembler");
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Nisso Biochem",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Spawns: " + ChatColor.DARK_BLUE + "Pig"));
        egg.setItemMeta(meta);
        return egg;
    }

    public static ItemStack loadSheepEgg() {
        ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1, EntityType.SHEEP.getTypeId());
        ItemMeta meta = egg.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "Bioassembler");
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Nisso Biochem",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Spawns: " + ChatColor.DARK_BLUE + "Sheep"));
        egg.setItemMeta(meta);
        return egg;
    }

    public static ItemStack loadChickenEgg() {
        ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1, EntityType.CHICKEN.getTypeId());
        ItemMeta meta = egg.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "Bioassembler");
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Nisso Biochem",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Spawns: " + ChatColor.DARK_BLUE + "Chicken"));
        egg.setItemMeta(meta);
        return egg;
    }

    private static final int CHEST_SLOTS = 27;

    public static List<ItemStack> buildCacheContents(Random rng, double cellFillChance,
                                                     String[] itemKeys, double[] itemThresholds) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < CHEST_SLOTS; slot++) {
            if (rng.nextDouble() >= cellFillChance) continue;
            double roll = rng.nextDouble();
            for (int j = 0; j < itemThresholds.length; j++) {
                if (roll < itemThresholds[j]) {
                    ItemStack item = resolveItem(itemKeys[j], rng);
                    if (item != null) items.add(item);
                    break;
                }
            }
        }
        return items;
    }

    private static ItemStack resolveItem(String key, Random rng) {
        switch (key) {
            case "cookies":             return loadCookies(poissonSample(rng, 1) + 1);
            case "monomolecular-blade": return loadMonomolecularBlade(poissonSample(rng, 50));
            case "water-bucket":        return loadWaterBucket();
            case "cow-egg":             return loadCowEgg();
            case "pig-egg":             return loadPigEgg();
            case "sheep-egg":           return loadSheepEgg();
            case "chicken-egg":         return loadChickenEgg();
            default:                    return null;
        }
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