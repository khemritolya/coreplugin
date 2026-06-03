package org.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;

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

    public static final String SPICE_NAME              = ChatColor.RESET + "" + ChatColor.BLUE + "Nacre";

    public static final String MONOMOLECULAR_BLADE_NAME = ChatColor.RESET + "" + ChatColor.DARK_RED + "Monomolecular Blade";
    public static final String PROSPECTOR_NAME         = ChatColor.RESET + "" + ChatColor.DARK_RED + "Prospector's Pickaxe";
    public static final String HARD_HAT_NAME           = ChatColor.RESET + "" + ChatColor.DARK_RED + "Hard Hat";
    public static final String IMPERIAL_TACHI_NAME     = ChatColor.RESET + "" + ChatColor.DARK_RED + "Imperial Tachi";
    public static final String NANO_CRYSTAL_NAME       = ChatColor.RESET + "" + ChatColor.DARK_RED + "Nano Carapace";

    public static final String SPEED_BOOTS_NAME           = ChatColor.RESET + "" + ChatColor.AQUA + "QuantumSuit Boots";
    public static final String PHASE_DEVICE_NAME          = ChatColor.RESET + "" + ChatColor.DARK_PURPLE + "Phase Device";
    public static final String PLASMA_CHARGE_NAME         = ChatColor.RESET + "" + ChatColor.AQUA + "Plasma Charge";

    public static final String UPLINK_CARD_NAME           = ChatColor.RESET + "" + ChatColor.YELLOW + "Uplink Card";
    public static final String DIAMONDOID_CHESTPLATE_NAME  = ChatColor.RESET + "" + ChatColor.AQUA + "Diamondoid Chestplate";
    public static final String DIAMONDOID_LEGGINGS_NAME    = ChatColor.RESET + "" + ChatColor.AQUA + "Diamondoid Leggings";
    public static final String DIAMONDOID_BOOTS_NAME       = ChatColor.RESET + "" + ChatColor.AQUA + "Diamondoid Boots";
    public static final String DIAMONDOID_SWORD_NAME       = ChatColor.RESET + "" + ChatColor.AQUA + "Diamondoid Sword";
    public static final String UPLINK_BEACON_DROP_NAME    = ChatColor.RESET + "" + ChatColor.YELLOW + "Hyperbridge Beacon";

    public static final String UPLINK_GUARDIAN_HEAD_NAME = ChatColor.RESET + "" + ChatColor.DARK_RED + "Uplink Guardian Head";

    public static ItemStack loadSpice(int mark, int duration) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        meta.setDisplayName(SPICE_NAME);

        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.SATURATION,     duration, mark,   false, false), true);
        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.REGENERATION,   duration, mark,   false, false), true);
        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, mark/2, false, false), true);
        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.NIGHT_VISION,   duration, 0,      false, false), true);

        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Length: " + ChatColor.AQUA + duration / 20 + "s",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Grade: " + ChatColor.GOLD + toRoman(mark + 1)));
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

    public static ItemStack loadHardHat() {
        ItemStack helmet = new ItemStack(Material.GOLD_HELMET);
        ItemMeta meta = helmet.getItemMeta();
        meta.setDisplayName(HARD_HAT_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Amakuni Concern",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Your Safety, Our Priority™"));
        helmet.setItemMeta(meta);
        helmet.addUnsafeEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 10);
        helmet.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
        return helmet;
    }

    public static ItemStack loadProspectorPickaxe() {
        ItemStack pick = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta meta = pick.getItemMeta();
        meta.setDisplayName(PROSPECTOR_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Amakuni Concern",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Hard Hat Required"));
        pick.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 10);
        pick.setItemMeta(meta);
        return pick;
    }

    public static ItemStack loadMonomolecularBlade() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(MONOMOLECULAR_BLADE_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Amakuni Concern",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "May Shatter Spontaneously"));
        sword.setItemMeta(meta);
        sword.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
        sword.setDurability((short) (Short.MAX_VALUE - 1));

        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(sword);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        NBTTagList modifiers = new NBTTagList();
        NBTTagCompound dmg = new NBTTagCompound();
        dmg.setString("AttributeName", "generic.attackDamage");
        dmg.setString("Name", "generic.attackDamage");
        dmg.setDouble("Amount", Short.MAX_VALUE);
        dmg.setInt("Operation", 0);
        dmg.setLong("UUIDMost", 894654L);
        dmg.setLong("UUIDLeast", 2872L);
        dmg.setString("Slot", "mainhand");
        modifiers.add(dmg);
        tag.set("AttributeModifiers", modifiers);
        nmsStack.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static ItemStack loadImperialTachi() {
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(IMPERIAL_TACHI_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Amakuni Concern",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "A Civilized Weapon"));
        sword.setItemMeta(meta);

        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(sword);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        tag.setByte("Unbreakable", (byte) 1);
        nmsStack.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static ItemStack loadPhaseDevice(Random rng) {
        double t = rng.nextDouble();
        long cooldownMs    = (long)(180_000 + t * 120_000); // 3–5 min
        long durationTicks = (long)(100     + t * 100);     // 5–10 sec

        int cooldownSec = (int)(cooldownMs / 1000);
        int durationSec = (int)(durationTicks / 20);

        ItemStack charge = new ItemStack(Material.FIREWORK_CHARGE);
        FireworkEffectMeta meta = (FireworkEffectMeta) charge.getItemMeta();
        meta.setDisplayName(PHASE_DEVICE_NAME);
        meta.setLore(Arrays.asList(
                ChatColor.RESET + "" + ChatColor.GRAY + "by Imperial High-Energy Lab",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Mildly Reality-Bending Bauble",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Length: " + ChatColor.AQUA + durationSec + "s",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.AQUA + cooldownSec + "s"));
        meta.setEffect(FireworkEffect.builder().withColor(org.bukkit.Color.PURPLE).build());
        charge.setItemMeta(meta);
        charge.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);

        java.util.UUID id = java.util.UUID.randomUUID();
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(charge);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        tag.setLong("PhaseIdMost",        id.getMostSignificantBits());
        tag.setLong("PhaseIdLeast",       id.getLeastSignificantBits());
        tag.setLong("PhaseReadyAt",       0L);
        tag.setLong("PhaseCooldownMs",    cooldownMs);
        tag.setLong("PhaseDurationTicks", durationTicks);
        nmsStack.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    private static final Material[] MUSIC_DISCS = {
        Material.GOLD_RECORD, Material.GREEN_RECORD, Material.RECORD_3,  Material.RECORD_4,
        Material.RECORD_5,    Material.RECORD_6,     Material.RECORD_7,  Material.RECORD_8,
        Material.RECORD_9,    Material.RECORD_10,    Material.RECORD_11, Material.RECORD_12
    };

    public static ItemStack loadMusicDisc(Random rng) {
        return new ItemStack(MUSIC_DISCS[rng.nextInt(MUSIC_DISCS.length)]);
    }

    public static ItemStack loadJukebox() {
        return new ItemStack(Material.JUKEBOX);
    }

    public static ItemStack loadSpeedBoots() {
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
        meta.setColor(org.bukkit.Color.WHITE);
        meta.setDisplayName(SPEED_BOOTS_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Imperial High-Energy Lab",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Bends Spacetime Slightly"));
        boots.setItemMeta(meta);
        boots.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
        boots.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 10);
        boots.addUnsafeEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 10);
        boots.addUnsafeEnchantment(Enchantment.PROTECTION_FALL, 10);
        return boots;
    }

    public static ItemStack loadPlasmaCharge(int amount) {
        ItemStack snowball = new ItemStack(Material.SNOW_BALL, amount);
        ItemMeta meta = snowball.getItemMeta();
        meta.setDisplayName(PLASMA_CHARGE_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Imperial High-Energy Lab",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "High-Energy Physics on Demand"));
        snowball.setItemMeta(meta);
        snowball.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 10);
        return snowball;
    }

    public static ItemStack loadNanoCrystal(Random rng) {
        // Exponential decay (λ=1) bucketed into 1–4 hearts; ~63% chance of 1, ~23% 2, ~9% 3, ~5% 4
        int hearts = Math.min(4, (int)(-Math.log(rng.nextDouble())) + 1);

        ItemStack crystal = new ItemStack(Material.PRISMARINE_CRYSTALS);
        ItemMeta meta = crystal.getItemMeta();
        meta.setDisplayName(NANO_CRYSTAL_NAME);
        meta.setLore(Arrays.asList(
                ChatColor.RESET + "" + ChatColor.GRAY + "by Amakuni Concern",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Little Things, Big Impact™",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Max Health: "
                        + ChatColor.YELLOW + "+" + hearts + (hearts == 1 ? " Heart" : " Hearts")));
        crystal.setItemMeta(meta);

        java.util.UUID id = java.util.UUID.randomUUID();
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(crystal);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        tag.setLong("NanoCrystalIdMost",  id.getMostSignificantBits());
        tag.setLong("NanoCrystalIdLeast", id.getLeastSignificantBits());
        tag.setByte("NanoCrystalHearts",  (byte) hearts);
        nmsStack.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static ItemStack loadWaterBucket() {
        return new ItemStack(Material.WATER_BUCKET);
    }

    public static ItemStack loadCowEgg() {
        ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1, EntityType.COW.getTypeId());
        ItemMeta meta = egg.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "Bioassembler");
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Nisso Biochem",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Spawns: " + ChatColor.GREEN + "Cow"));
        egg.setItemMeta(meta);
        return egg;
    }

    public static ItemStack loadPigEgg() {
        ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1, EntityType.PIG.getTypeId());
        ItemMeta meta = egg.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "Bioassembler");
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Nisso Biochem",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Spawns: " + ChatColor.GREEN + "Pig"));
        egg.setItemMeta(meta);
        return egg;
    }

    public static ItemStack loadSheepEgg() {
        ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1, EntityType.SHEEP.getTypeId());
        ItemMeta meta = egg.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "Bioassembler");
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Nisso Biochem",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Spawns: " + ChatColor.GREEN + "Sheep"));
        egg.setItemMeta(meta);
        return egg;
    }

    public static ItemStack loadChickenEgg() {
        ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1, EntityType.CHICKEN.getTypeId());
        ItemMeta meta = egg.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "Bioassembler");
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Nisso Biochem",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Spawns: " + ChatColor.GREEN + "Chicken"));
        egg.setItemMeta(meta);
        return egg;
    }

    public static List<ItemStack> buildCacheContents(
            Random rng,
            int rareMax,   double rareFillChance,   String[] rareKeys,   double[] rareThresholds,
            int commonMax, double commonFillChance, String[] commonKeys, double[] commonThresholds) {
        List<ItemStack> items = new ArrayList<>();
        addItems(rng, rareMax,   rareFillChance,   rareKeys,   rareThresholds,   items);
        addItems(rng, commonMax, commonFillChance, commonKeys, commonThresholds, items);
        return items;
    }

    private static void addItems(Random rng, int max, double fillChance,
                                  String[] keys, double[] thresholds, List<ItemStack> out) {
        for (int i = 0; i < max; i++) {
            if (rng.nextDouble() >= fillChance) continue;
            double roll = rng.nextDouble();
            for (int j = 0; j < thresholds.length; j++) {
                if (roll < thresholds[j]) {
                    ItemStack item = resolveItem(keys[j], rng);
                    if (item != null) out.add(item);
                    break;
                }
            }
        }
    }

    private static ItemStack resolveItem(String key, Random rng) {
        switch (key) {
            case "name-tag":            return new ItemStack(Material.NAME_TAG);
            case "cookies":             return loadCookies(poissonSample(rng, 1) + 1);
            case "hard-hat":            return loadHardHat();
            case "prospector-pickaxe":  return loadProspectorPickaxe();
            case "monomolecular-blade": return loadMonomolecularBlade();
            case "saddle":              return new ItemStack(Material.SADDLE);
            case "music-disc":          return loadMusicDisc(rng);
            case "jukebox":             return loadJukebox();
            case "nacre": {
                int mark = poissonSample(rng, 3.0);
                double bonus = rng.nextDouble() + rng.nextDouble() + rng.nextDouble() + rng.nextDouble() + rng.nextDouble();
                int duration = (int) (3600 * (0.75 + bonus * 0.1));
                return loadSpice(mark, duration);
            }
            case "phase-device":        return loadPhaseDevice(rng);
            case "speed-boots":         return loadSpeedBoots();
            case "imperial-tachi":      return loadImperialTachi();
            case "plasma-charge":       return loadPlasmaCharge(poissonSample(rng, 1) + 1);
            case "nano-crystal":        return loadNanoCrystal(rng);
            case "uplink-card":         return loadUplinkCard();
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
        meta.setTitle(ChatColor.RESET + "" + ChatColor.AQUA + player.getName() + ChatColor.RESET + "'s Data Pad");
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

    public static ItemStack loadBed() {
        ItemStack bed = new ItemStack(Material.BED);
        ItemMeta meta = bed.getItemMeta();
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by New Fuji Co. Ltd.",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Click to Set Spawn"));
        bed.setItemMeta(meta);
        return bed;
    }

    public static ItemStack loadUplinkCard() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.setDisplayName(UPLINK_CARD_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by New Fuji Co. Ltd.",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Click on an Obsidian",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Uplink Beacon to Activate"));
        paper.setItemMeta(meta);

        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(paper);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        tag.setByte("UplinkCard", (byte) 1);
        tag.setString("UplinkCardId", java.util.UUID.randomUUID().toString());
        nmsStack.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static ItemStack loadDiamondoidChestplate() {
        ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta meta = chest.getItemMeta();
        meta.setDisplayName(DIAMONDOID_CHESTPLATE_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Amakuni Concern",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "The Ash of the Samurai Beats",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Strong within Imperial Hearts"));
        chest.setItemMeta(meta);
        chest.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 10);

        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(chest);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        tag.setByte("Unbreakable", (byte) 1);
        nmsStack.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static ItemStack loadDiamondoidSword() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(DIAMONDOID_SWORD_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Amakuni Concern",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "The Ash of the Samurai Beats",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Strong within Imperial Hearts"));
        sword.setItemMeta(meta);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 10);

        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(sword);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        tag.setByte("Unbreakable", (byte) 1);
        nmsStack.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static ItemStack loadDiamondoidLeggings() {
        ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemMeta meta = legs.getItemMeta();
        meta.setDisplayName(DIAMONDOID_LEGGINGS_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Amakuni Concern",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "The Ash of the Samurai Beats",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Strong within Imperial Hearts"));
        legs.setItemMeta(meta);
        legs.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 10);

        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(legs);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        tag.setByte("Unbreakable", (byte) 1);
        nmsStack.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static ItemStack loadDiamondoidBoots() {
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        ItemMeta meta = boots.getItemMeta();
        meta.setDisplayName(DIAMONDOID_BOOTS_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by Amakuni Concern",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "The Ash of the Samurai Beats",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Strong within Imperial Hearts"));
        boots.setItemMeta(meta);
        boots.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 10);

        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(boots);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        tag.setByte("Unbreakable", (byte) 1);
        nmsStack.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static ItemStack loadUplinkGuardianHead() {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 1);
        ItemMeta meta = skull.getItemMeta();
        meta.setDisplayName(UPLINK_GUARDIAN_HEAD_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by New Fuji Co. Ltd.",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "System Error: " + ChatColor.RED + ChatColor.MAGIC + "AAAAA",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Gives Immunity to Knockback"));
        skull.setItemMeta(meta);

        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(skull);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();
        NBTTagList modifiers = new NBTTagList();
        NBTTagCompound kbRes = new NBTTagCompound();
        kbRes.setString("AttributeName", "generic.knockbackResistance");
        kbRes.setString("Name", "generic.knockbackResistance");
        kbRes.setDouble("Amount", 1.0);
        kbRes.setInt("Operation", 0);
        kbRes.setLong("UUIDMost", 0x4B6F636B42616B21L);
        kbRes.setLong("UUIDLeast", 0x4865616448656164L);
        kbRes.setString("Slot", "head");
        modifiers.add(kbRes);
        tag.set("AttributeModifiers", modifiers);
        tag.setString("UplinkGuardianHeadId", java.util.UUID.randomUUID().toString());
        nmsStack.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static ItemStack loadUplinkBeaconDrop() {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();
        meta.setDisplayName(UPLINK_BEACON_DROP_NAME);
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.GRAY + "by New Fuji Co. Ltd.",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Range: " + ChatColor.YELLOW + "10m",
                ChatColor.RESET + "" + ChatColor.DARK_GRAY + "Destination: " + ChatColor.RED + "Unknown"));
        star.setItemMeta(meta);
        return star;
    }
}