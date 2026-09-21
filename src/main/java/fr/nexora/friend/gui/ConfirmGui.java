package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ConfirmGui extends NexoraGui {

    private final String message;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmGui(NexoraFriend plugin, Player viewer, String message, Runnable onConfirm, Runnable onCancel) {
        super(plugin, viewer);
        this.message = message;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    public void build() {
        String title = guiConfig().getString("confirm.title", "&8⚠ Confirmation");
        int size = guiConfig().getInt("confirm.size", 27);
        if (inventory == null) {
            inventory = createChestInventory(MessageUtils.color(title), size);
        } else {
            inventory.clear();
        }

        inventory.setItem(4, ItemBuilder.of(Material.PAPER)
                .name(MessageUtils.color("&e⚠ " + message))
                .build());

        int confirmSlot = guiConfig().getInt("confirm.confirm-slot", 11);
        int cancelSlot = guiConfig().getInt("confirm.cancel-slot", 15);
        Material confirmMaterial = materialOr(guiConfig().getString("confirm.confirm-material"), Material.LIME_WOOL);
        Material cancelMaterial = materialOr(guiConfig().getString("confirm.cancel-material"), Material.RED_WOOL);

        inventory.setItem(confirmSlot, ItemBuilder.of(confirmMaterial)
                .name(MessageUtils.color("&a✔ CONFIRMER"))
                .pdcString(actionKey(), GuiActions.CONFIRM_YES)
                .build());

        inventory.setItem(cancelSlot, ItemBuilder.of(cancelMaterial)
                .name(MessageUtils.color("&c✖ ANNULER"))
                .pdcString(actionKey(), GuiActions.CONFIRM_NO)
                .build());
    }

    private Material materialOr(String name, Material fallback) {
        Material material = name != null ? Material.matchMaterial(name) : null;
        return material != null ? material : fallback;
    }

    @Override
    protected void handleCustomClick(String action, String data, InventoryClickEvent event) {
        if (GuiActions.CONFIRM_YES.equals(action)) {
            onConfirm.run();
        } else if (GuiActions.CONFIRM_NO.equals(action)) {
            if (onCancel != null) {
                onCancel.run();
            } else {
                plugin.guiManager().back(viewer);
            }
        }
    }
}
