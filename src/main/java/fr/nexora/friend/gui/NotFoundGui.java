package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class NotFoundGui extends NexoraGui {

    public NotFoundGui(NexoraFriend plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public void build() {
        if (inventory == null) {
            inventory = createChestInventory(MessageUtils.color("&8Joueur introuvable"), 27);
        } else {
            inventory.clear();
        }

        inventory.setItem(13, ItemBuilder.of(Material.BARRIER)
                .name(MessageUtils.color("&c❌ JOUEUR INTROUVABLE"))
                .lore(List.of(
                        MessageUtils.color("&7Aucun joueur correspondant"),
                        MessageUtils.color("&7à cette recherche n'a été trouvé.")))
                .build());

        inventory.setItem(11, ItemBuilder.of(Material.COMPASS)
                .name(MessageUtils.color("&b🔎 Nouvelle recherche"))
                .pdcString(actionKey(), GuiActions.NEW_SEARCH)
                .build());

        placeBackButtonAt(15);
    }

    @Override
    protected void handleCustomClick(String action, String data, InventoryClickEvent event) {
        if (GuiActions.NEW_SEARCH.equals(action)) {
            SearchGui.open(plugin, viewer);
        }
    }
}
