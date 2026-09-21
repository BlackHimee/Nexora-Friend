package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.model.RequestResult;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RequestDetailGui extends NexoraGui {

    public enum Mode {
        INCOMING, OUTGOING
    }

    private final UUID target;
    private final String targetName;
    private final Mode mode;

    public RequestDetailGui(NexoraFriend plugin, Player viewer, UUID target, String targetName, Mode mode) {
        super(plugin, viewer);
        this.target = target;
        this.targetName = targetName;
        this.mode = mode;
    }

    @Override
    public void build() {
        boolean incoming = mode == Mode.INCOMING;
        String titleKey = incoming ? "request-detail.title-incoming" : "request-detail.title-outgoing";
        String title = guiConfig().getString(titleKey, "&8Demande").replace("%player%", targetName);
        int size = guiConfig().getInt("request-detail.size", 27);
        if (inventory == null) {
            inventory = createChestInventory(MessageUtils.color(title), size);
        } else {
            inventory.clear();
        }

        int headSlot = guiConfig().getInt("request-detail.head-slot", 13);
        inventory.setItem(headSlot, ItemBuilder.playerHead(target, targetName)
                .name(MessageUtils.color("&b" + targetName))
                .lore(List.of(MessageUtils.color(incoming
                        ? "&7" + targetName + " souhaite vous ajouter"
                        : "&7Demande en attente envoyée à " + targetName)))
                .build());

        if (incoming) {
            inventory.setItem(guiConfig().getInt("request-detail.accept-slot", 11), ItemBuilder.of(Material.LIME_WOOL)
                    .name(MessageUtils.color("&a✔ Accepter"))
                    .pdcString(actionKey(), GuiActions.FRIEND_ACCEPT_REQUEST)
                    .pdcString(dataKey(), target.toString())
                    .build());
            inventory.setItem(guiConfig().getInt("request-detail.deny-slot", 15), ItemBuilder.of(Material.RED_WOOL)
                    .name(MessageUtils.color("&c✖ Refuser"))
                    .pdcString(actionKey(), GuiActions.FRIEND_DENY_REQUEST)
                    .pdcString(dataKey(), target.toString())
                    .build());
            inventory.setItem(guiConfig().getInt("request-detail.block-slot", 16), ItemBuilder.of(Material.BARRIER)
                    .name(MessageUtils.color("&4🚫 Bloquer"))
                    .pdcString(actionKey(), GuiActions.FRIEND_BLOCK)
                    .pdcString(dataKey(), target.toString())
                    .build());
        } else {
            inventory.setItem(guiConfig().getInt("request-detail.cancel-slot", 15), ItemBuilder.of(Material.RED_WOOL)
                    .name(MessageUtils.color("&c↩ Annuler la demande"))
                    .pdcString(actionKey(), GuiActions.FRIEND_CANCEL_REQUEST)
                    .pdcString(dataKey(), target.toString())
                    .build());
        }

        placeBackButtonAt(guiConfig().getInt("request-detail.back-slot", 22));
    }

    @Override
    protected void handleCustomClick(String action, String data, InventoryClickEvent event) {
        switch (action) {
            case GuiActions.FRIEND_ACCEPT_REQUEST -> plugin.scheduler().sync(
                    plugin.requestManager().acceptRequest(viewer, target), this::onRequestOutcome);
            case GuiActions.FRIEND_DENY_REQUEST -> plugin.scheduler().sync(
                    plugin.requestManager().denyRequest(viewer, target), this::onRequestOutcome);
            case GuiActions.FRIEND_CANCEL_REQUEST -> plugin.scheduler().sync(
                    plugin.requestManager().cancelRequest(viewer, target), this::onRequestOutcome);
            case GuiActions.FRIEND_BLOCK -> {
                if (checkPermission("nexora.friend.block")) {
                    plugin.scheduler().sync(plugin.blockManager().block(viewer.getUniqueId(), target), (v, throwable) -> {
                        if (throwable != null) {
                            plugin.notificationManager().error(viewer);
                            return;
                        }
                        plugin.messageUtils().send(viewer, "player-blocked", Map.of("player", targetName));
                        plugin.guiManager().back(viewer);
                    });
                }
            }
            default -> {
            }
        }
    }

    private void onRequestOutcome(RequestResult result, Throwable throwable) {
        if (throwable != null || result != RequestResult.SUCCESS) {
            plugin.notificationManager().error(viewer);
        }
        plugin.guiManager().back(viewer);
    }
}
