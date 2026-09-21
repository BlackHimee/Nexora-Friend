package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.model.AddPrivacy;
import fr.nexora.friend.model.ProfilePrivacy;
import fr.nexora.friend.model.SocialProfile;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class SettingsGui extends NexoraGui {

    public SettingsGui(NexoraFriend plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public void build() {
        String title = guiConfig().getString("settings.title", "&8Paramètres");
        int size = guiConfig().getInt("settings.size", 45);
        if (inventory == null) {
            inventory = createChestInventory(MessageUtils.color(title), size);
        } else {
            inventory.clear();
        }

        SocialProfile profile = plugin.profileManager().getCached(viewer.getUniqueId());
        if (profile == null) {
            placeBackButton();
            placeHomeButton();
            return;
        }

        placeToggle("settings.buttons.notifications", GuiActions.SETTINGS_TOGGLE_NOTIFICATIONS, profile.notificationsEnabled());
        placeToggle("settings.buttons.online", GuiActions.SETTINGS_TOGGLE_ONLINE, profile.notifyOnline());
        placeToggle("settings.buttons.offline", GuiActions.SETTINGS_TOGGLE_OFFLINE, profile.notifyOffline());
        placeToggle("settings.buttons.requests", GuiActions.SETTINGS_TOGGLE_REQUESTS, profile.notifyRequests());
        placeCycle("settings.buttons.add-privacy", GuiActions.SETTINGS_CYCLE_ADD_PRIVACY, describe(profile.addPrivacy()));
        placeCycle("settings.buttons.profile-privacy", GuiActions.SETTINGS_CYCLE_PROFILE_PRIVACY, describe(profile.profilePrivacy()));
        placeConfigButton("settings.buttons.blocked", GuiActions.OPEN_BLOCKED, null);

        placeBackButton();
        placeHomeButton();
    }

    private void placeToggle(String path, String action, boolean enabled) {
        String base = guiConfig().getString(path + ".name", "");
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String stateLine = enabled ? "&a✔ Activé" : "&c✖ Désactivé";
        int slot = guiConfig().getInt(path + ".slot", 0);
        inventory.setItem(slot, ItemBuilder.of(material)
                .name(MessageUtils.color(base))
                .lore(List.of(MessageUtils.color(stateLine), MessageUtils.color("&eCliquez pour changer")))
                .pdcString(actionKey(), action)
                .build());
    }

    private void placeCycle(String path, String action, String currentValue) {
        String base = guiConfig().getString(path + ".name", "");
        Material material = Material.matchMaterial(guiConfig().getString(path + ".material", "PAPER"));
        if (material == null) {
            material = Material.PAPER;
        }
        int slot = guiConfig().getInt(path + ".slot", 0);
        inventory.setItem(slot, ItemBuilder.of(material)
                .name(MessageUtils.color(base))
                .lore(List.of(MessageUtils.color("&7Actuel: &f" + currentValue), MessageUtils.color("&eCliquez pour changer")))
                .pdcString(actionKey(), action)
                .build());
    }

    private String describe(AddPrivacy privacy) {
        return switch (privacy) {
            case EVERYONE -> "Tout le monde";
            case FRIENDS_OF_FRIENDS -> "Amis d'amis";
            case NOBODY -> "Personne";
        };
    }

    private String describe(ProfilePrivacy privacy) {
        return switch (privacy) {
            case EVERYONE -> "Tout le monde";
            case FRIENDS_ONLY -> "Amis uniquement";
            case NOBODY -> "Personne";
        };
    }

    @Override
    protected void handleCustomClick(String action, String data, InventoryClickEvent event) {
        SocialProfile profile = plugin.profileManager().getCached(viewer.getUniqueId());
        if (profile == null) {
            return;
        }

        switch (action) {
            case GuiActions.SETTINGS_TOGGLE_NOTIFICATIONS -> profile.notificationsEnabled(!profile.notificationsEnabled());
            case GuiActions.SETTINGS_TOGGLE_ONLINE -> profile.notifyOnline(!profile.notifyOnline());
            case GuiActions.SETTINGS_TOGGLE_OFFLINE -> profile.notifyOffline(!profile.notifyOffline());
            case GuiActions.SETTINGS_TOGGLE_REQUESTS -> profile.notifyRequests(!profile.notifyRequests());
            case GuiActions.SETTINGS_CYCLE_ADD_PRIVACY -> profile.addPrivacy(nextAddPrivacy(profile.addPrivacy()));
            case GuiActions.SETTINGS_CYCLE_PROFILE_PRIVACY -> profile.profilePrivacy(nextProfilePrivacy(profile.profilePrivacy()));
            case GuiActions.OPEN_BLOCKED -> {
                plugin.guiManager().open(viewer, new BlockedPlayersGui(plugin, viewer));
                return;
            }
            default -> {
                return;
            }
        }

        plugin.profileManager().save(profile);
        refresh();
    }

    private AddPrivacy nextAddPrivacy(AddPrivacy current) {
        AddPrivacy[] values = AddPrivacy.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    private ProfilePrivacy nextProfilePrivacy(ProfilePrivacy current) {
        ProfilePrivacy[] values = ProfilePrivacy.values();
        return values[(current.ordinal() + 1) % values.length];
    }
}
