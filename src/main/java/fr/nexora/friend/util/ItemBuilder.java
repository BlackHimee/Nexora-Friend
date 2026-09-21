package fr.nexora.friend.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;

import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder playerHead(UUID uuid, String name) {
        return new ItemBuilder(Material.PLAYER_HEAD).skullOwner(Bukkit.createProfile(uuid, name));
    }

    public ItemBuilder name(Component name) {
        meta.displayName(name.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder lore(List<Component> lore) {
        meta.lore(lore.stream()
                .map(line -> line.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                .toList());
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder pdcString(NamespacedKey key, String value) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        return this;
    }

    public ItemBuilder skullOwner(PlayerProfile profile) {
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwnerProfile(profile);
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
