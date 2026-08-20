package com.nous.progression;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class EnglishProgression extends JavaPlugin {
    private static EnglishProgression instance;
    private File dataFile;
    private YamlConfiguration data;
    private Map<String, Double> multipliers;
    private Map<String, Double> thresholds;
    private List<String> levelupMessages;
    private String trackName;
    private boolean promoteOnLevelup;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfig();
        loadData();
        getLogger().info("EnglishProgression enabled! Track: " + trackName + " | " + thresholds.size() + " thresholds");
    }

    private void loadConfig() {
        multipliers = new LinkedHashMap<>();
        var cfg = getConfig();
        for (String key : cfg.getConfigurationSection("multipliers").getKeys(false)) {
            multipliers.put(key.toLowerCase(), cfg.getDouble("multipliers." + key));
        }
        thresholds = new LinkedHashMap<>();
        for (String key : cfg.getConfigurationSection("thresholds").getKeys(false)) {
            thresholds.put(key.toLowerCase(), cfg.getDouble("thresholds." + key));
        }
        levelupMessages = cfg.getStringList("levelup-messages");
        trackName = cfg.getString("luckperms.track", "english");
        promoteOnLevelup = cfg.getBoolean("luckperms.promote-on-levelup", true);
    }

    private void loadData() {
        dataFile = new File(getDataFolder(), "earnings.yml");
        if (!dataFile.exists()) {
            try { dataFile.getParentFile().mkdirs(); dataFile.createNewFile(); }
            catch (IOException e) { getLogger().warning("Cannot create earnings.yml"); }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    // === Static API ===

    public static EnglishProgression get() { return instance; }

    /** Get earnings multiplier for player's current level */
    public static double getMultiplier(Player player) {
        if (instance == null) return 1.0;
        String level = instance.getPlayerLevel(player);
        return instance.multipliers.getOrDefault(level, 1.0);
    }

    /** Add earnings and check for level-up. Returns new level if leveled up, null otherwise. */
    public static String addEarnings(Player player, double amount) {
        if (instance == null) return null;
        return instance.addEarningsInternal(player, amount);
    }

    // === Internal ===

    private String addEarningsInternal(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        double current = data.getDouble(uuid.toString(), 0.0);
        double newTotal = current + amount;
        data.set(uuid.toString(), newTotal);
        try { data.save(dataFile); } catch (IOException e) { getLogger().warning("Cannot save earnings"); }

        // Check level-ups
        String currentLevel = getPlayerLevel(player);
        String highestLevel = currentLevel;

        for (Map.Entry<String, Double> entry : thresholds.entrySet()) {
            if (newTotal >= entry.getValue() && compareLevels(entry.getKey(), highestLevel) > 0) {
                highestLevel = entry.getKey();
            }
        }

        if (!highestLevel.equals(currentLevel) && promoteOnLevelup) {
            promotePlayer(player, highestLevel);
            sendLevelUpMessage(player, currentLevel, highestLevel);
            return highestLevel;
        }
        return null;
    }

    private void promotePlayer(Player player, String targetLevel) {
        try {
            LuckPerms lp = Bukkit.getServicesManager().load(LuckPerms.class);
            if (lp == null) return;
            Track track = lp.getTrackManager().getTrack(trackName);
            if (track == null) return;

            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) return;

            List<String> groups = track.getGroups();
            String current = getPlayerLevel(player);
            int currentIdx = groups.indexOf(current);
            int targetIdx = groups.indexOf(targetLevel);

            if (targetIdx > currentIdx) {
                // Promote step by step
                for (int i = currentIdx + 1; i <= targetIdx; i++) {
                    String cmd = "lp user " + player.getName() + " parent add " + groups.get(i);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                getLogger().info(player.getName() + " leveled up: " + current + " → " + targetLevel);
            }
        } catch (Exception e) {
            getLogger().warning("Failed to promote " + player.getName() + ": " + e.getMessage());
        }
    }

    private void sendLevelUpMessage(Player player, String from, String to) {
        double mult = multipliers.getOrDefault(to, 1.0);
        if (levelupMessages.isEmpty()) return;

        Random r = new Random();
        String template = levelupMessages.get(r.nextInt(levelupMessages.size()));
        String msg = String.format(template, from.toUpperCase(), to.toUpperCase(), mult);
        player.sendMessage(LEGACY.deserialize(msg));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    public String getPlayerLevel(Player player) {
        try {
            LuckPerms lp = Bukkit.getServicesManager().load(LuckPerms.class);
            if (lp != null) {
                User user = lp.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (prefix != null) {
                        String clean = prefix.replaceAll("[§&][0-9a-fA-Fk-oK-OrR]", "");
                        java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("\\[([A-C][0-2]|D1)\\]").matcher(clean);
                        if (m.find()) return m.group(1).toLowerCase();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "a0";
    }

    private int compareLevels(String a, String b) {
        List<String> order = new ArrayList<>(thresholds.keySet());
        return Integer.compare(order.indexOf(a), order.indexOf(b));
    }

    @Override
    public void onDisable() {
        getLogger().info("EnglishProgression disabled!");
        instance = null;
    }
}
