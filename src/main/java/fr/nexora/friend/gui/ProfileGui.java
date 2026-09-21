package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.model.RelationState;
import fr.nexora.friend.model.RequestResult;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProfileGui extends NexoraGui {

    private final UUID target;
    private final String targetName;

    public ProfileGui(NexoraFriend plugin, Player viewer, UUID target, String targetName) {
        super(plugin, viewer);
        this.target = target;
        this.targetName = targetName;
    }

    @Override
    public void build() {
        String title = guiConfig().getString("profile.title", "&8Profil de %player%").replace("%player%", targetName);
        int size = guiConfig().getInt("profile.size", 27);
        if (inventory == null) {
            inventory = createChestInventory(MessageUtils.color(title), size);
        } else {
            inventory.clear();
        }

        boolean online = Bukkit.getPlayer(target) != null;
        int friendCount = plugin.friendManager().getFriendCount(target);

        List<Component> headLore = new ArrayList<>();
        headLore.add(MessageUtils.color(online ? "&a🟢 EN LIGNE" : "&c🔴 HORS LIGNE"));
        headLore.add(Component.empty());
        headLore.add(MessageUtils.color("&7Amis: &f" + friendCount));

        inventory.setItem(guiConfig().getInt("profile.head-slot", 13), ItemBuilder.playerHead(target, targetName)
                .name(MessageUtils.color("&d" + targetName))
                .lore(headLore)
                .build());

        boolean self = target.equals(viewer.getUniqueId());
        if (!self) {
            placeRelationButtons(plugin.requestManager().getRelation(viewer.getUniqueId(), target));
        }

        placeBackButtonAt(guiConfig().getInt("profile.back-slot", 22));
    }

    private void placeRelationButtons(RelationState relation) {
        int primarySlot = guiConfig().getInt("profile.primary-slot", 11);
        int secondarySlot = guiConfig().getInt("profile.secondary-slot", 15);
        int blockSlot = guiConfig().getInt("profile.block-slot", 16);

        switch (relation) {
            case NONE -> {
                inventory.setItem(primarySlot, button(Material.LIME_DYE, "&a🤝 Ajouter en ami", GuiActions.FRIEND_ADD));
                inventory.setItem(blockSlot, button(Material.BARRIER, "&4🚫 Bloquer", GuiActions.FRIEND_BLOCK));
            }
            case PENDING_OUTGOING -> {
                inventory.setItem(primarySlot, informational(Material.PAPER, "&e📨 Demande envoyée"));
                inventory.setItem(secondarySlot, button(Material.RED_WOOL, "&c↩ Annuler", GuiActions.FRIEND_CANCEL_REQUEST));
                inventory.setItem(blockSlot, button(Material.BARRIER, "&4🚫 Bloquer", GuiActions.FRIEND_BLOCK));
            }
            case PENDING_INCOMING -> {
                inventory.setItem(primarySlot, button(Material.LIME_WOOL, "&a✔ Accepter", GuiActions.FRIEND_ACCEPT_REQUEST));
                inventory.setItem(secondarySlot, button(Material.RED_WOOL, "&c✖ Refuser", GuiActions.FRIEND_DENY_REQUEST));
                inventory.setItem(blockSlot, button(Material.BARRIER, "&4🚫 Bloquer", GuiActions.FRIEND_BLOCK));
            }
            case FRIENDS -> {
                inventory.setItem(primarySlot, informational(Material.LIME_DYE, "&a🤝 Déjà ami"));
                inventory.setItem(secondarySlot, button(Material.RED_WOOL, "&c🗑 Supprimer", GuiActions.FRIEND_REMOVE));
                inventory.setItem(blockSlot, button(Material.BARRIER, "&4🚫 Bloquer", GuiActions.FRIEND_BLOCK));
            }
            case BLOCKED -> inventory.setItem(primarySlot, button(Material.LIME_DYE, "&a🔓 Débloquer", GuiActions.FRIEND_UNBLOCK));
            case BLOCKED_BY -> inventory.setItem(primarySlot, informational(Material.BARRIER, "&cCe joueur vous a bloqué"));
        }
    }

    private ItemStack button(Material material, String name, String action) {
        return ItemBuilder.of(material)
                .name(MessageUtils.color(name))
                .pdcString(actionKey(), action)
                .pdcString(dataKey(), target.toString())
                .build();
    }

    private ItemStack informational(Material material, String name) {
        return ItemBuilder.of(material)
                .name(MessageUtils.color(name))
                .pdcString(actionKey(), GuiActions.NOOP)
                .build();
    }

    @Override
    protected void handleCustomClick(String action, String data, InventoryClickEvent event) {
        switch (action) {
            case GuiActions.FRIEND_ADD -> {
                if (checkPermission("nexora.friend.add")) {
                    plugin.scheduler().sync(plugin.requestManager().sendRequest(viewer, target), (result, throwable) -> {
                        if (throwable != null) {
                            plugin.notificationManager().error(viewer);
                        } else if (result != RequestResult.SUCCESS) {
                            plugin.notificationManager().sendRequestFailure(viewer, result, targetName, plugin.friendManager().getLimit(viewer));
                        }
                        refresh();
                    });
                }
            }
            case GuiActions.FRIEND_CANCEL_REQUEST -> {
                if (checkPermission("nexora.friend.requests")) {
                    plugin.scheduler().sync(plugin.requestManager().cancelRequest(viewer, target), (result, throwable) -> refresh());
                }
            }
            case GuiActions.FRIEND_ACCEPT_REQUEST -> {
                if (checkPermission("nexora.friend.requests")) {
                    plugin.scheduler().sync(plugin.requestManager().acceptRequest(viewer, target), (result, throwable) -> refresh());
                }
            }
            case GuiActions.FRIEND_DENY_REQUEST -> {
                if (checkPermission("nexora.friend.requests")) {
                    plugin.scheduler().sync(plugin.requestManager().denyRequest(viewer, target), (result, throwable) -> refresh());
                }
            }
            case GuiActions.FRIEND_REMOVE -> {
                if (checkPermission("nexora.friend.remove")) {
                    plugin.guiManager().open(viewer, new ConfirmGui(plugin, viewer,
                            "Supprimer " + targetName + " de vos amis ?",
                            () -> plugin.scheduler().sync(
                                    plugin.friendManager().removeFriendship(viewer.getUniqueId(), target, viewer.getName(), targetName),
                                    (v, throwable) -> plugin.guiManager().back(viewer)),
                            () -> plugin.guiManager().back(viewer)));
                }
            }
            case GuiActions.FRIEND_BLOCK -> {
                if (checkPermission("nexora.friend.block")) {
                    plugin.scheduler().sync(plugin.blockManager().block(viewer.getUniqueId(), target), (v, throwable) -> {
                        if (throwable != null) {
                            plugin.notificationManager().error(viewer);
                        } else {
                            plugin.messageUtils().send(viewer, "player-blocked", Map.of("player", targetName));
                        }
                        refresh();
                    });
                }
            }
            case GuiActions.FRIEND_UNBLOCK -> {
                if (checkPermission("nexora.friend.block")) {
                    plugin.scheduler().sync(plugin.blockManager().unblock(viewer.getUniqueId(), target), (v, throwable) -> {
                        if (throwable != null) {
                            plugin.notificationManager().error(viewer);
                        } else {
                            plugin.messageUtils().send(viewer, "player-unblocked", Map.of("player", targetName));
                        }
                        refresh();
                    });
                }
            }
            default -> {
            }
        }
    }
}
