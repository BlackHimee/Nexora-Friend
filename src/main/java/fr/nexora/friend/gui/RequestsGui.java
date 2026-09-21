package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class RequestsGui extends NexoraGui {

    public RequestsGui(NexoraFriend plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public void build() {
        String title = guiConfig().getString("requests.title", "&8Demandes d'amis");
        int size = guiConfig().getInt("requests.size", 27);
        if (inventory == null) {
            inventory = createChestInventory(MessageUtils.color(title), size);
        } else {
            inventory.clear();
        }

        int receivedCount = plugin.requestManager().getIncoming(viewer.getUniqueId()).size();
        int sentCount = plugin.requestManager().getOutgoing(viewer.getUniqueId()).size();

        int receivedSlot = guiConfig().getInt("requests.received-slot", 11);
        int sentSlot = guiConfig().getInt("requests.sent-slot", 15);
        Material receivedMaterial = materialOr(guiConfig().getString("requests.received-material"), Material.WRITABLE_BOOK);
        Material sentMaterial = materialOr(guiConfig().getString("requests.sent-material"), Material.PAPER);

        inventory.setItem(receivedSlot, ItemBuilder.of(receivedMaterial)
                .name(MessageUtils.color("&e📨 Demandes reçues"))
                .lore(List.of(
                        MessageUtils.color("&7Vous avez &f" + receivedCount + " &7demande(s) en attente."),
                        MessageUtils.color("&eCliquez pour ouvrir")))
                .pdcString(actionKey(), GuiActions.OPEN_RECEIVED_REQUESTS)
                .build());

        inventory.setItem(sentSlot, ItemBuilder.of(sentMaterial)
                .name(MessageUtils.color("&e📤 Demandes envoyées"))
                .lore(List.of(
                        MessageUtils.color("&7Vous avez &f" + sentCount + " &7demande(s) envoyée(s)."),
                        MessageUtils.color("&eCliquez pour ouvrir")))
                .pdcString(actionKey(), GuiActions.OPEN_SENT_REQUESTS)
                .build());

        placeBackButton();
        placeHomeButton();
    }

    private Material materialOr(String name, Material fallback) {
        Material material = name != null ? Material.matchMaterial(name) : null;
        return material != null ? material : fallback;
    }

    @Override
    protected void handleCustomClick(String action, String data, InventoryClickEvent event) {
        switch (action) {
            case GuiActions.OPEN_RECEIVED_REQUESTS ->
                    plugin.guiManager().open(viewer, new RequestDetailListGui(plugin, viewer, RequestDetailListGui.Mode.INCOMING));
            case GuiActions.OPEN_SENT_REQUESTS ->
                    plugin.guiManager().open(viewer, new RequestDetailListGui(plugin, viewer, RequestDetailListGui.Mode.OUTGOING));
            default -> {
            }
        }
    }
}
