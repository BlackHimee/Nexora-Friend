package fr.nexora.friend.util;

import fr.nexora.friend.NexoraFriend;
import org.bukkit.Bukkit;

import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;

/**
 * Bridges async database work back onto the main server thread.
 */
public class Scheduler {

    private final NexoraFriend plugin;

    public Scheduler(NexoraFriend plugin) {
        this.plugin = plugin;
    }

    public void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public <T> void sync(CompletableFuture<T> future, BiConsumer<T, Throwable> consumer) {
        future.whenComplete((result, throwable) -> runSync(() -> consumer.accept(result, throwable)));
    }
}
