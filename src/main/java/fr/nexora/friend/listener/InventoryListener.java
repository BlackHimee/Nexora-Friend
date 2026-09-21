package fr.nexora.friend.listener;

import fr.nexora.friend.gui.NexoraGui;
import fr.nexora.friend.gui.SearchGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Single entry point for every Nexora-Friend inventory. Cancels all item
 * movement unconditionally (no drag, no shift-click, no hotbar swap, no
 * double click can ever remove or move a GUI item) and routes legitimate
 * clicks to the owning {@link NexoraGui} or {@link SearchGui}.
 */
public class InventoryListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();

        if (holder instanceof NexoraGui gui) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
                return;
            }
            gui.dispatchClick(event);
        } else if (holder instanceof SearchGui searchGui) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(top)) {
                searchGui.handleClick(event);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof NexoraGui || holder instanceof SearchGui) {
            event.setCancelled(true);
        }
    }
}
