package fr.nexora.friend.manager;

import fr.nexora.friend.hook.LuckPermsHook;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Resolves the friend limit for a player from nexora.friend.limit.* permissions.
 * The highest limit among all granted permissions wins automatically.
 */
public class PermissionManager {

    private final ConfigManager configManager;
    private final LuckPermsHook luckPermsHook;

    public PermissionManager(ConfigManager configManager, LuckPermsHook luckPermsHook) {
        this.configManager = configManager;
        this.luckPermsHook = luckPermsHook;
    }

    public int getFriendLimit(Player player) {
        int limit = configManager.config().getInt("friends.default-limit", 50);

        ConfigurationSection limits = configManager.config().getConfigurationSection("friends.limits");
        if (limits == null) {
            return limit;
        }

        for (String permission : limits.getKeys(false)) {
            if (hasPermission(player, permission)) {
                limit = Math.max(limit, limits.getInt(permission));
            }
        }

        return limit;
    }

    private boolean hasPermission(Player player, String permission) {
        if (luckPermsHook != null) {
            return luckPermsHook.hasPermission(player.getUniqueId(), permission);
        }
        return player.hasPermission(permission);
    }
}
