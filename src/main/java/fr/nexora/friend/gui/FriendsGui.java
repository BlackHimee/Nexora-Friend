package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.model.SocialProfile;
import fr.nexora.friend.util.ItemBuilder;
import fr.nexora.friend.util.MessageUtils;
import fr.nexora.friend.util.Pagination;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class FriendsGui extends NexoraGui {

    public FriendsGui(NexoraFriend plugin, Player viewer) {
        super(plugin, viewer);
    }

    @Override
    public void build() {
        String title = guiConfig().getString("friends.title", "&8Mes amis");
        int size = guiConfig().getInt("friends.size", 54);
        if (inventory == null) {
            inventory = createChestInventory(MessageUtils.color(title), size);
        } else {
            inventory.clear();
        }

        int pageSize = plugin.configManager().config().getInt("pagination.friends-per-page", 36);

        List<UUID> friends = new ArrayList<>(plugin.friendManager().getFriends(viewer.getUniqueId()));
        friends.sort(Comparator
                .comparing((UUID u) -> !plugin.profileManager().isOnline(u))
                .thenComparing(this::displayNameFor, String.CASE_INSENSITIVE_ORDER));

        int pageCount = Pagination.pageCount(friends.size(), pageSize);
        page = Math.max(0, Math.min(page, pageCount - 1));

        List<UUID> slice = Pagination.slice(friends, page, pageSize);
        int slot = 0;
        for (UUID friendUuid : slice) {
            inventory.setItem(slot++, buildFriendHead(friendUuid));
        }

        placeBackButton();
        placeHomeButton();
        if (page > 0) {
            placePrevPageButton();
        }
        if (page < pageCount - 1) {
            placeNextPageButton();
        }
    }

    private String displayNameFor(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        SocialProfile profile = plugin.profileManager().getCached(uuid);
        if (profile != null) {
            return profile.lastKnownName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : uuid.toString();
    }

    private ItemStack buildFriendHead(UUID friendUuid) {
        boolean online = plugin.profileManager().isOnline(friendUuid);
        String name = displayNameFor(friendUuid);

        List<Component> lore = new ArrayList<>();
        if (online) {
            lore.add(MessageUtils.color(guiConfig().getString("friends.lore-online", "&7Statut: &a&lEn ligne")));
        } else {
            lore.add(MessageUtils.color(guiConfig().getString("friends.lore-offline", "&7Statut: &c&lHors ligne")));
            String time = formatLastSeen(friendUuid);
            String lastSeenLine = guiConfig().getString("friends.lore-last-seen", "&7Dernière connexion: &f%time%").replace("%time%", time);
            lore.add(MessageUtils.color(lastSeenLine));
        }

        return ItemBuilder.playerHead(friendUuid, name)
                .name(MessageUtils.color("&b" + name))
                .lore(lore)
                .pdcString(actionKey(), GuiActions.PLAYER_HEAD)
                .pdcString(dataKey(), friendUuid.toString())
                .build();
    }

    private String formatLastSeen(UUID uuid) {
        SocialProfile profile = plugin.profileManager().getCached(uuid);
        Instant lastSeen = profile != null ? profile.lastSeen() : null;
        if (lastSeen == null) {
            long millis = Bukkit.getOfflinePlayer(uuid).getLastPlayed();
            if (millis <= 0) {
                return "?";
            }
            lastSeen = Instant.ofEpochMilli(millis);
        }
        return formatDuration(Duration.between(lastSeen, Instant.now()));
    }

    private String formatDuration(Duration duration) {
        if (duration.isNegative()) {
            duration = Duration.ZERO;
        }
        long days = duration.toDays();
        if (days > 0) {
            return days + "j";
        }
        long hours = duration.toHours();
        if (hours > 0) {
            return hours + "h";
        }
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return minutes + "min";
        }
        return "< 1min";
    }

    @Override
    protected void handleCustomClick(String action, String data, InventoryClickEvent event) {
        if (GuiActions.PLAYER_HEAD.equals(action) && data != null && checkPermission("nexora.friend.profile")) {
            UUID target = UUID.fromString(data);
            String name = displayNameFor(target);
            plugin.guiManager().open(viewer, new ProfileGui(plugin, viewer, target, name));
        }
    }
}
