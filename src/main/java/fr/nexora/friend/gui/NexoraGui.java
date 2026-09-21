package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Base class for every Nexora-Friend inventory GUI.
 * <p>
 * Handles the shared plumbing: item identification via PersistentDataContainer,
 * universal navigation buttons and safe in-place refreshing so async data can
 * populate a GUI without flickering or losing the player's cursor position.
 */
public abstract class NexoraGui implements InventoryHolder {

    protected final NexoraFriend plugin;
    protected final Player viewer;
    protected Inventory inventory;
    protected int page = 0;

    protected NexoraGui(NexoraFriend plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    /** (Re)populates {@link #inventory}, creating it on first call. */
    public abstract void build();

    protected abstract void handleCustomClick(String action, String data, InventoryClickEvent event);

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player viewer() {
        return viewer;
    }

    public void open() {
        build();
        viewer.openInventory(inventory);
        plugin.soundUtil().play(viewer, "gui-open");
    }

    /** Rebuilds the same live inventory in place - safe to call after an async load completes. */
    public void refresh() {
        build();
    }

    public boolean isCurrentlyOpen() {
        Inventory top = viewer.getOpenInventory().getTopInventory();
        return top.getHolder() == this;
    }

    /** Entry point called by the global inventory listener once the click has been cancelled. */
    public final void dispatchClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }
        String action = meta.getPersistentDataContainer().get(actionKey(), PersistentDataType.STRING);
        if (action == null) {
            return;
        }
        String data = meta.getPersistentDataContainer().get(dataKey(), PersistentDataType.STRING);
        handleClick(event, action, data);
    }

    private void handleClick(InventoryClickEvent event, String action, String data) {
        switch (action) {
            case GuiActions.NAV_BACK -> plugin.guiManager().back(viewer);
            case GuiActions.NAV_HOME -> plugin.guiManager().openMain(viewer);
            case GuiActions.NAV_CLOSE -> viewer.closeInventory();
            case GuiActions.NAV_PREV_PAGE -> {
                if (page > 0) {
                    page--;
                    refresh();
                }
            }
            case GuiActions.NAV_NEXT_PAGE -> {
                page++;
                refresh();
            }
            case GuiActions.NOOP -> {
                // Decorative item - intentionally does nothing.
            }
            default -> handleCustomClick(action, data, event);
        }
    }

    protected Inventory createChestInventory(Component title, int size) {
        int clamped = Math.max(9, Math.min(54, (size / 9) * 9));
        return Bukkit.createInventory(this, clamped, title);
    }

    protected NamespacedKey actionKey() {
        return new NamespacedKey(plugin, "nf_action");
    }

    protected NamespacedKey dataKey() {
        return new NamespacedKey(plugin, "nf_data");
    }

    protected ConfigurationSection guiConfig() {
        return plugin.configManager().gui();
    }

    protected void placeConfigButton(String path, String action, String data) {
        ConfigurationSection section = guiConfig();
        if (section == null || !section.isConfigurationSection(path)) {
            return;
        }
        int slot = section.getInt(path + ".slot", -1);
        if (slot < 0 || inventory == null || slot >= inventory.getSize()) {
            return;
        }

        Material material = Material.matchMaterial(section.getString(path + ".material", "STONE"));
        if (material == null) {
            material = Material.BARRIER;
        }

        String name = section.getString(path + ".name", "");
        List<String> lore = section.getStringList(path + ".lore");

        ItemBuilder builder = ItemBuilder.of(material)
                .name(MessageUtils.color(name))
                .lore(lore.stream().map(MessageUtils::color).toList())
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .pdcString(actionKey(), action);

        if (data != null) {
            builder.pdcString(dataKey(), data);
        }

        inventory.setItem(slot, builder.build());
    }

    protected void placeBackButton() {
        placeConfigButton("navigation.back", GuiActions.NAV_BACK, null);
    }

    protected void placeBackButtonAt(int slot) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, ItemBuilder.of(Material.ARROW)
                .name(MessageUtils.color("&c← Retour"))
                .pdcString(actionKey(), GuiActions.NAV_BACK)
                .build());
    }

    protected void placeHomeButton() {
        placeConfigButton("navigation.home", GuiActions.NAV_HOME, null);
    }

    protected void placeCloseButton() {
        placeConfigButton("navigation.close", GuiActions.NAV_CLOSE, null);
    }

    protected void placePrevPageButton() {
        placeConfigButton("navigation.previous-page", GuiActions.NAV_PREV_PAGE, null);
    }

    protected void placeNextPageButton() {
        placeConfigButton("navigation.next-page", GuiActions.NAV_NEXT_PAGE, null);
    }

    /** Guards an action behind a permission node, notifying the viewer if denied. */
    protected boolean checkPermission(String permission) {
        if (viewer.hasPermission(permission)) {
            return true;
        }
        plugin.messageUtils().send(viewer, "no-permission");
        plugin.soundUtil().play(viewer, "error");
        return false;
    }

    protected void fill(Material material) {
        if (inventory == null) {
            return;
        }
        ItemBuilder filler = ItemBuilder.of(material).name(Component.empty()).pdcString(actionKey(), GuiActions.NOOP);
        var item = filler.build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item);
            }
        }
    }
}
