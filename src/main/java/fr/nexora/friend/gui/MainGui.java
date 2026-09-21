package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.util.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MainGui extends NexoraGui {

    public MainGui(NexoraFriend plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public void build() {
        String title = guiConfig().getString("main.title", "&8Nexora Social");
        int size = guiConfig().getInt("main.size", 54);
        if (inventory == null) {
            inventory = createChestInventory(MessageUtils.color(title), size);
        } else {
            inventory.clear();
        }

        if (guiConfig().getBoolean("main.filler.enabled", true)) {
            Material filler = Material.matchMaterial(guiConfig().getString("main.filler.material", "BLACK_STAINED_GLASS_PANE"));
            fill(filler != null ? filler : Material.BLACK_STAINED_GLASS_PANE);
        }

        placeConfigButton("main.buttons.friends", GuiActions.OPEN_FRIENDS, null);
        placeConfigButton("main.buttons.requests", GuiActions.OPEN_REQUESTS, null);
        placeConfigButton("main.buttons.online", GuiActions.OPEN_ONLINE, null);
        placeConfigButton("main.buttons.search", GuiActions.OPEN_SEARCH, null);
        placeConfigButton("main.buttons.profile", GuiActions.OPEN_SELF_PROFILE, null);
        placeConfigButton("main.buttons.settings", GuiActions.OPEN_SETTINGS, null);
    }

    @Override
    protected void handleCustomClick(String action, String data, InventoryClickEvent event) {
        switch (action) {
            case GuiActions.OPEN_FRIENDS -> plugin.guiManager().open(viewer, new FriendsGui(plugin, viewer));
            case GuiActions.OPEN_REQUESTS -> {
                if (checkPermission("nexora.friend.requests")) {
                    plugin.guiManager().open(viewer, new RequestsGui(plugin, viewer));
                }
            }
            case GuiActions.OPEN_ONLINE -> plugin.guiManager().open(viewer, new OnlinePlayersGui(plugin, viewer));
            case GuiActions.OPEN_SEARCH -> {
                if (checkPermission("nexora.friend.search")) {
                    SearchGui.open(plugin, viewer);
                }
            }
            case GuiActions.OPEN_SELF_PROFILE -> {
                if (checkPermission("nexora.friend.profile")) {
                    plugin.guiManager().open(viewer, new ProfileGui(plugin, viewer, viewer.getUniqueId(), viewer.getName()));
                }
            }
            case GuiActions.OPEN_SETTINGS -> {
                if (checkPermission("nexora.friend.settings")) {
                    plugin.guiManager().open(viewer, new SettingsGui(plugin, viewer));
                }
            }
            default -> {
            }
        }
    }
}
