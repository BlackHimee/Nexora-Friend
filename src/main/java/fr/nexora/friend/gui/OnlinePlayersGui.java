package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import fr.nexora.friend.util.Pagination;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class OnlinePlayersGui extends NexoraGui {

    public OnlinePlayersGui(NexoraFriend plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public void build() {
        String title = guiConfig().getString("online-players.title", "&8Joueurs connectés");
        int size = guiConfig().getInt("online-players.size", 54);
        if (inventory == null) {
            inventory = createChestInventory(MessageUtils.color(title), size);
        } else {
            inventory.clear();
        }

        int pageSize = plugin.configManager().config().getInt("pagination.online-players-per-page", 36);

        List<Player> players = plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(viewer.getUniqueId()))
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int pageCount = Pagination.pageCount(players.size(), pageSize);
        page = Math.max(0, Math.min(page, pageCount - 1));

        List<Player> slice = Pagination.slice(players, page, pageSize);
        int slot = 0;
        for (Player online : slice) {
            inventory.setItem(slot++, buildHead(online));
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

    private ItemStack buildHead(Player online) {
        return ItemBuilder.playerHead(online.getUniqueId(), online.getName())
                .name(MessageUtils.color("&a" + online.getName()))
                .lore(List.of(MessageUtils.color("&7Cliquez pour voir son profil")))
                .pdcString(actionKey(), GuiActions.PLAYER_HEAD)
                .pdcString(dataKey(), online.getUniqueId().toString())
                .build();
    }

    @Override
    protected void handleCustomClick(String action, String data, InventoryClickEvent event) {
        if (GuiActions.PLAYER_HEAD.equals(action) && data != null && checkPermission("nexora.friend.profile")) {
            UUID target = UUID.fromString(data);
            Player online = plugin.getServer().getPlayer(target);
            String name = online != null ? online.getName() : "?";
            plugin.guiManager().open(viewer, new ProfileGui(plugin, viewer, target, name));
        }
    }
}
