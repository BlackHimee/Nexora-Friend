package fr.nexora.friend.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.UUID;

/**
 * Thin wrapper around the LuckPerms API. Only instantiated when LuckPerms is
 * present on the server (see {@link #isAvailable()}); the rest of the plugin
 * must keep working with plain Bukkit permissions otherwise.
 */
public class LuckPermsHook {

    private final LuckPerms api;

    public LuckPermsHook() {
        this.api = LuckPermsProvider.get();
    }

    public static boolean isAvailable() {
        return org.bukkit.Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }

    public boolean hasPermission(UUID uuid, String permission) {
        User user = api.getUserManager().getUser(uuid);
        if (user == null) {
            return false;
        }
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }
}
