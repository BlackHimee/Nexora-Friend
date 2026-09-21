package fr.nexora.friend.manager;

import fr.nexora.friend.NexoraFriend;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads and hot-reloads config.yml, messages.yml and gui.yml.
 */
public class ConfigManager {

    private final NexoraFriend plugin;

    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration gui;

    private File configFile;
    private File messagesFile;
    private File guiFile;

    public ConfigManager(NexoraFriend plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.getDataFolder().mkdirs();

        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        guiFile = new File(plugin.getDataFolder(), "gui.yml");

        saveDefaultIfMissing("config.yml", configFile);
        saveDefaultIfMissing("messages.yml", messagesFile);
        saveDefaultIfMissing("gui.yml", guiFile);

        config = loadWithDefaults(configFile, "config.yml");
        messages = loadWithDefaults(messagesFile, "messages.yml");
        gui = loadWithDefaults(guiFile, "gui.yml");
    }

    public void reload() {
        load();
    }

    private void saveDefaultIfMissing(String resourcePath, File target) {
        if (!target.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private FileConfiguration loadWithDefaults(File file, String resourcePath) {
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);

        try (InputStream defaultStream = plugin.getResource(resourcePath)) {
            if (defaultStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                loaded.setDefaults(defaults);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load embedded defaults for " + resourcePath + ": " + e.getMessage());
        }

        return loaded;
    }

    public FileConfiguration config() {
        return config;
    }

    public FileConfiguration messages() {
        return messages;
    }

    public FileConfiguration gui() {
        return gui;
    }

    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }
}
