package fr.nexora.friend.util;

import fr.nexora.friend.manager.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    private final ConfigManager configManager;

    public SoundUtil(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void play(Player player, String key) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!configManager.config().getBoolean("sounds.enabled", true)) {
            return;
        }
        String soundName = configManager.config().getString("sounds." + key);
        if (soundName == null || soundName.isBlank()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ignored) {
            // Invalid sound configured - silently skip rather than spamming the console per click.
        }
    }
}
