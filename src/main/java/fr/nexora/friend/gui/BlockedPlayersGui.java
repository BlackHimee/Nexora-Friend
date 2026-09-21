package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import fr.nexora.friend.util.Pagination;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlockedPlayersGui extends NexoraGui {

    public BlockedPlayersGui(NexoraFriend plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public void build() {
        String title = guiConfig().getString("blocked-players.title", "&8Joueurs bloqués");
        int size = guiConfig().getInt("blocked-players.size", 54);
        if (inventory == null) {
            inventory = createChestInventory(MessageUtils.color(title), size);
        } else {
            inventory.clear();
        }

        List<UUID> blocked = new ArrayList<>(plugin.blockManager().getBlocked(viewer.getUniqueId()));
        blocked.sort((a, b) -> nameOf(a).compareToIgnoreCase(nameOf(b)));

        int pageSize = size - 9;
        int pageCount = Pagination.pageCount(blocked.size(), pageSize);
        page = Math.max(0, Math.min(page, pageCount - 1));

        List<UUID> slice = Pagination.slice(blocked, page, pageSize);
        int slot = 0;
        for (UUID uuid : slice) {
            inventory.setItem(slot++, buildHead(uuid));
        }

        placeBackButton();
        placeHomeButton();
        if (page > 0) {
            placePrevPageButton();
        }
        if (page < pageCount - 1) {
            placeNextPageButton();
        }
    }

    private String nameOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        var profile = plugin.profileManager().getCached(uuid);
        if (profile != null) {
            return profile.lastKnownName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : uuid.toString();
    }

    private ItemStack buildHead(UUID uuid) {
        String name = nameOf(uuid);
        return ItemBuilder.playerHead(uuid, name)
                .name(MessageUtils.color("&c" + name))
                .lore(List.of(MessageUtils.color("&7Cliquez pour débloquer")))
                .pdcString(actionKey(), GuiActions.BLOCKED_HEAD)
                .pdcString(dataKey(), uuid.toString())
                .build();
    }

    @Override
    protected void handleCustomClick(String action, String data, InventoryClickEvent event) {
        if (!GuiActions.BLOCKED_HEAD.equals(action) || data == null || !checkPermission("nexora.friend.block")) {
            return;
        }
        UUID target = UUID.fromString(data);
        String name = nameOf(target);
        plugin.scheduler().sync(plugin.blockManager().unblock(viewer.getUniqueId(), target), (v, throwable) -> {
            if (throwable != null) {
                plugin.notificationManager().error(viewer);
            } else {
                plugin.messageUtils().send(viewer, "player-unblocked", Map.of("player", name));
            }
            refresh();
        });
    }
}
