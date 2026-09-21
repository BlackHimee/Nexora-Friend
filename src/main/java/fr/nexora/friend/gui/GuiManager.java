package fr.nexora.friend.gui;

import fr.nexora.friend.NexoraFriend;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player GUI navigation history so "back" and "home" always work,
 * without leaking closed sessions.
 */
public class GuiManager {

    private final NexoraFriend plugin;
    private final Map<UUID, Deque<NexoraGui>> history = new ConcurrentHashMap<>();

    public GuiManager(NexoraFriend plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, NexoraGui gui) {
        history.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>()).push(gui);
        gui.open();
    }

    /** Replaces the whole navigation stack, used when opening /social fresh. */
    public void openMain(Player player) {
        history.remove(player.getUniqueId());
        open(player, new MainGui(plugin, player));
    }

    public void back(Player player) {
        Deque<NexoraGui> stack = history.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            openMain(player);
            return;
        }

        stack.pop();
        NexoraGui previous = stack.peek();
        if (previous == null) {
            openMain(player);
            return;
        }

        previous.open();
    }

    public void clear(UUID uuid) {
        history.remove(uuid);
    }
}
