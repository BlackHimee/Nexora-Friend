package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * Player search implemented with a virtual Anvil inventory: the player types a
 * name into the rename field and clicks the output slot to submit. Because
 * every click on this inventory is cancelled by {@code InventoryListener},
 * confirming a search never actually consumes the item or costs XP.
 */
public class SearchGui implements InventoryHolder {

    private static final int RESULT_SLOT = 2;

    private final NexoraFriend plugin;
    private final Player viewer;
    private final Inventory inventory;

    private SearchGui(NexoraFriend plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        String title = plugin.configManager().gui().getString("search.title", "&8Rechercher un joueur");
        this.inventory = Bukkit.createInventory(this, InventoryType.ANVIL, MessageUtils.color(title));
    }

    public static void open(NexoraFriend plugin, Player viewer) {
        SearchGui gui = new SearchGui(plugin, viewer);
        gui.inventory.setItem(0, ItemBuilder.of(Material.PAPER).build());
        viewer.openInventory(gui.inventory);
        plugin.soundUtil().play(viewer, "gui-open");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void handleClick(InventoryClickEvent event) {
        if (event.getSlot() != RESULT_SLOT) {
            return;
        }
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) {
            return;
        }
        ItemMeta meta = result.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }
        String query = PlainTextComponentSerializer.plainText().serialize(meta.displayName()).trim();
        if (query.isEmpty()) {
            return;
        }
        viewer.closeInventory();
        resolveAndOpen(plugin, viewer, query);
    }

    public static void resolveAndOpen(NexoraFriend plugin, Player viewer, String query) {
        plugin.scheduler().sync(plugin.profileManager().resolveByName(query), (uuidOpt, throwable) -> {
            if (throwable != null) {
                plugin.notificationManager().error(viewer);
                return;
            }
            if (uuidOpt.isPresent()) {
                UUID uuid = uuidOpt.get();
                plugin.guiManager().open(viewer, new ProfileGui(plugin, viewer, uuid, resolveName(plugin, uuid, query)));
            } else {
                plugin.guiManager().open(viewer, new NotFoundGui(plugin, viewer));
            }
        });
    }

    private static String resolveName(NexoraFriend plugin, UUID uuid, String fallback) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        var profile = plugin.profileManager().getCached(uuid);
        if (profile != null) {
            return profile.lastKnownName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : fallback;
    }
}
