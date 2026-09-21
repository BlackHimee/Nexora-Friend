package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.model.FriendRequest;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import fr.nexora.friend.util.Pagination;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class RequestDetailListGui extends NexoraGui {

    public enum Mode {
        INCOMING, OUTGOING
    }

    private final Mode mode;

    public RequestDetailListGui(NexoraFriend plugin, Player viewer, Mode mode) {
        super(plugin, viewer);
        this.mode = mode;
    }

    @Override
    public void build() {
        boolean incoming = mode == Mode.INCOMING;
        String configKey = incoming ? "received-requests" : "sent-requests";
        String title = guiConfig().getString(configKey + ".title", incoming ? "&8Demandes reçues" : "&8Demandes envoyées");
        int size = guiConfig().getInt(configKey + ".size", 54);
        if (inventory == null) {
            inventory = createChestInventory(MessageUtils.color(title), size);
        } else {
            inventory.clear();
        }

        List<FriendRequest> requests = incoming
                ? plugin.requestManager().getIncoming(viewer.getUniqueId())
                : plugin.requestManager().getOutgoing(viewer.getUniqueId());

        int pageSize = size - 9;
        int pageCount = Pagination.pageCount(requests.size(), pageSize);
        page = Math.max(0, Math.min(page, pageCount - 1));

        List<FriendRequest> slice = Pagination.slice(requests, page, pageSize);
        int slot = 0;
        for (FriendRequest request : slice) {
            UUID other = incoming ? request.requester() : request.target();
            inventory.setItem(slot++, buildHead(other));
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

    private ItemStack buildHead(UUID other) {
        String name = nameOf(other);
        String action = mode == Mode.INCOMING ? GuiActions.INCOMING_REQUEST_HEAD : GuiActions.OUTGOING_REQUEST_HEAD;
        String lore = mode == Mode.INCOMING
                ? "&7" + name + " souhaite vous ajouter à sa liste d'amis."
                : "&7Demande en attente envoyée à &f" + name + "&7.";

        return ItemBuilder.playerHead(other, name)
                .name(MessageUtils.color("&b" + name))
                .lore(List.of(MessageUtils.color(lore), MessageUtils.color("&eCliquez pour ouvrir")))
                .pdcString(actionKey(), action)
                .pdcString(dataKey(), other.toString())
                .build();
    }

    @Override
    protected void handleCustomClick(String action, String data, InventoryClickEvent event) {
        if (data == null) {
            return;
        }
        UUID other = UUID.fromString(data);
        String name = nameOf(other);

        if (GuiActions.INCOMING_REQUEST_HEAD.equals(action)) {
            plugin.guiManager().open(viewer, new RequestDetailGui(plugin, viewer, other, name, RequestDetailGui.Mode.INCOMING));
        } else if (GuiActions.OUTGOING_REQUEST_HEAD.equals(action)) {
            plugin.guiManager().open(viewer, new RequestDetailGui(plugin, viewer, other, name, RequestDetailGui.Mode.OUTGOING));
        }
    }
}
