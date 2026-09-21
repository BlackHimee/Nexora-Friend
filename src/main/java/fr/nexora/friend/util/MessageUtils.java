package fr.nexora.friend.util;

import fr.nexora.friend.manager.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class MessageUtils {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final ConfigManager configManager;

    public MessageUtils(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public static Component color(String raw) {
        return LEGACY.deserialize(raw == null ? "" : raw);
    }

    public String raw(String key) {
        return configManager.messages().getString(key, key);
    }

    public Component get(String key) {
        return get(key, Map.of());
    }

    public Component get(String key, Map<String, String> placeholders) {
        String prefix = configManager.messages().getString("prefix", "");
        return color(prefix + apply(raw(key), placeholders));
    }

    public Component getNoPrefix(String key, Map<String, String> placeholders) {
        return color(apply(raw(key), placeholders));
    }

    private String apply(String message, Map<String, String> placeholders) {
        if (message == null) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(get(key, placeholders));
    }
}
