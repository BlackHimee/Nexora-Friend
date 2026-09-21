package fr.nexora.friend;

import fr.nexora.friend.command.SocialAdminCommand;
import fr.nexora.friend.command.SocialCommand;
import fr.nexora.friend.database.DatabaseManager;
import fr.nexora.friend.gui.GuiManager;
import fr.nexora.friend.hook.LuckPermsHook;
import fr.nexora.friend.hook.PlaceholderAPIHook;
import fr.nexora.friend.listener.InventoryListener;
import fr.nexora.friend.listener.PlayerJoinListener;
import fr.nexora.friend.listener.PlayerQuitListener;
import fr.nexora.friend.manager.BlockManager;
import fr.nexora.friend.manager.CacheManager;
import fr.nexora.friend.manager.ConfigManager;
import fr.nexora.friend.manager.FriendManager;
import fr.nexora.friend.manager.NotificationManager;
import fr.nexora.friend.manager.PermissionManager;
import fr.nexora.friend.manager.ProfileManager;
import fr.nexora.friend.manager.RequestManager;
import fr.nexora.friend.util.MessageUtils;
import fr.nexora.friend.util.Scheduler;
import fr.nexora.friend.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class NexoraFriend extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private Scheduler scheduler;
    private CacheManager cacheManager;
    private MessageUtils messageUtils;
    private SoundUtil soundUtil;

    private LuckPermsHook luckPermsHook;
    private boolean placeholderApiEnabled;

    private PermissionManager permissionManager;
    private ProfileManager profileManager;
    private NotificationManager notificationManager;
    private FriendManager friendManager;
    private BlockManager blockManager;
    private RequestManager requestManager;

    private GuiManager guiManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        configManager.load();

        this.scheduler = new Scheduler(this);
        this.cacheManager = new CacheManager();
        this.messageUtils = new MessageUtils(configManager);
        this.soundUtil = new SoundUtil(configManager);

        this.databaseManager = new DatabaseManager(this);
        try {
            databaseManager.init();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize the database - disabling Nexora-Friend.");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        setupHooks();

        this.permissionManager = new PermissionManager(configManager, luckPermsHook);
        this.profileManager = new ProfileManager(databaseManager.profileDao(), cacheManager, configManager);
        this.notificationManager = new NotificationManager(configManager, messageUtils, soundUtil, cacheManager);
        this.friendManager = new FriendManager(databaseManager.friendDao(), cacheManager, permissionManager, notificationManager, scheduler);
        this.blockManager = new BlockManager(databaseManager.blockDao(), databaseManager.friendDao(), databaseManager.requestDao(), cacheManager);
        this.requestManager = new RequestManager(databaseManager.requestDao(), cacheManager, friendManager, blockManager,
                profileManager, notificationManager, configManager, scheduler);

        this.guiManager = new GuiManager(this);

        registerListeners();
        registerCommands();

        if (PlaceholderAPIHook.isAvailable() && configManager.config().getBoolean("placeholderapi.enabled", true)) {
            new PlaceholderAPIHook(this).register();
            placeholderApiEnabled = true;
            getLogger().info("PlaceholderAPI hook registered.");
        }

        getLogger().info("Nexora-Friend enabled.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("Nexora-Friend disabled.");
    }

    private void setupHooks() {
        if (configManager.config().getBoolean("luckperms.enabled", true) && LuckPermsHook.isAvailable()) {
            try {
                this.luckPermsHook = new LuckPermsHook();
                getLogger().info("LuckPerms hook enabled.");
            } catch (Exception e) {
                getLogger().warning("LuckPerms was detected but the hook failed to initialize: " + e.getMessage());
            }
        }
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
    }

    private void registerCommands() {
        var socialCommand = getCommand("social");
        if (socialCommand != null) {
            socialCommand.setExecutor(new SocialCommand(this));
        }

        var adminCommand = getCommand("socialadmin");
        if (adminCommand != null) {
            SocialAdminCommand executor = new SocialAdminCommand(this);
            adminCommand.setExecutor(executor);
            adminCommand.setTabCompleter(executor);
        }
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public CacheManager cacheManager() {
        return cacheManager;
    }

    public MessageUtils messageUtils() {
        return messageUtils;
    }

    public SoundUtil soundUtil() {
        return soundUtil;
    }

    public LuckPermsHook luckPermsHook() {
        return luckPermsHook;
    }

    public boolean placeholderApiEnabled() {
        return placeholderApiEnabled;
    }

    public PermissionManager permissionManager() {
        return permissionManager;
    }

    public ProfileManager profileManager() {
        return profileManager;
    }

    public NotificationManager notificationManager() {
        return notificationManager;
    }

    public FriendManager friendManager() {
        return friendManager;
    }

    public BlockManager blockManager() {
        return blockManager;
    }

    public RequestManager requestManager() {
        return requestManager;
    }

    public GuiManager guiManager() {
        return guiManager;
    }
}
