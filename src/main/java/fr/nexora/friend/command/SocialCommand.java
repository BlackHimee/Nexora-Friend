package fr.nexora.friend.command;

import fr.nexora.friend.NexoraFriend;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SocialCommand implements CommandExecutor {

    private final NexoraFriend plugin;

    public SocialCommand(NexoraFriend plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messageUtils().send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("nexora.friend.use")) {
            plugin.messageUtils().send(player, "no-permission");
            return true;
        }

        plugin.guiManager().openMain(player);
        return true;
    }
}
