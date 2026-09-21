# Nexora-Friend

A complete, GUI-driven social/friend system for Paper 1.21+.

Players do almost everything through `/social` and inventory menus: friend
list, incoming/outgoing requests, online players, player search (Anvil GUI),
profiles, blocking and per-player privacy/notification settings. All data is
UUID-keyed, persisted asynchronously to SQLite or MySQL/MariaDB, and cached
in memory so the GUIs never block the main thread.

## Installation

1. Drop `Nexora-Friend-1.0.0.jar` into your server's `plugins/` folder.
2. Start the server once to generate `config.yml`, `messages.yml` and
   `gui.yml` inside `plugins/Nexora-Friend/`.
3. Edit the configuration files as needed (database type, permissions-based
   friend limits, messages, GUI layout, sounds...).
4. `/socialadmin reload` reloads config/messages/gui without a restart.

## Dependencies

- **Paper 1.21+** (Java 21). Uses modern Paper/Adventure APIs (Component
  item names/lore, `Bukkit#createProfile`, async-safe offline player
  lookups) - not vanilla Spigot compatible.
- **LuckPerms** (optional, soft-dependency). When present, friend-limit
  permission checks go through the LuckPerms API directly; otherwise plain
  Bukkit permissions are used. Either way, group names are never hardcoded -
  only `nexora.friend.limit.*` permission nodes matter.
- **PlaceholderAPI** (optional, soft-dependency). Registers the
  `nexorafriend` expansion when present.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/social` | Opens the main Nexora Social menu. | `nexora.friend.use` |
| `/socialadmin reload` | Reloads config.yml / messages.yml / gui.yml. | `nexora.friend.admin.reload` |
| `/socialadmin info <player>` | Shows a player's social stats. | `nexora.friend.admin.info` |
| `/socialadmin friends <player>` | Lists a player's friends in chat. | `nexora.friend.admin` |
| `/socialadmin remove <player> <friend>` | Force-removes a friendship. | `nexora.friend.admin.remove` |
| `/socialadmin clear <player>` | Clears a player's entire friend list. | `nexora.friend.admin.clear` |
| `/socialadmin debug` | Prints runtime diagnostics. | `nexora.friend.admin` |

Everyday play never requires typed commands beyond `/social` - the rest is
menus (Mes amis, Demandes, Joueurs connectés, Rechercher, Mon profil,
Paramètres).

## Permissions

Feature gates (all default `true`, restrict per-rank as needed):
`nexora.friend.use`, `.add`, `.remove`, `.search`, `.requests`, `.profile`,
`.block`, `.settings`.

Friend limits (default `false`, grant per rank - the **highest** matching
limit always wins, no group-name coupling):
`nexora.friend.limit.100`, `.300`, `.400`, `.500` (plus
`friends.default-limit` in config.yml for players with none of these).

Admin: `nexora.friend.admin` (parent) plus `.reload`, `.info`, `.remove`,
`.clear`.

## Configuration

- **config.yml** - database, friend limits, cooldown, notification toggles,
  default privacy, sounds, pagination page sizes.
- **messages.yml** - every player-facing message, legacy `&` color codes,
  `%player%` / `%limit%` / `%count%` placeholders.
- **gui.yml** - titles, sizes, slots, materials, names and lore for every
  menu button.

### MySQL / MariaDB

```yaml
database:
  type: MYSQL   # or MARIADB
  mysql:
    host: localhost
    port: 3306
    database: nexora_friend
    username: root
    password: "your-password"
    pool:
      maximum-pool-size: 10
```

Tables (`nf_profiles`, `nf_friends`, `nf_requests`, `nf_blocks`) are created
automatically on first connection.

### SQLite

Default out of the box - stores everything in
`plugins/Nexora-Friend/database.db`. Fine for small/medium servers; switch
to MySQL/MariaDB for larger networks or multi-server setups sharing one
friend database.

## Architecture

```
gui/            Inventory GUIs (NexoraGui base class, GuiManager navigation stack)
manager/        Business logic: FriendManager, RequestManager, BlockManager,
                ProfileManager, NotificationManager, PermissionManager, CacheManager
database/       HikariCP-backed SQLite/MySQL implementations + async DAOs
model/          Friend, FriendRequest, Block, SocialProfile, enums
listener/       Inventory security + join/quit data loading
hook/           LuckPerms / PlaceholderAPI integration (both optional)
command/        /social, /socialadmin
util/           ItemBuilder, MessageUtils, Pagination, Scheduler, SoundUtil
```

Every database operation is asynchronous (`CompletableFuture`, dedicated
executor) and results are only applied to Bukkit API / inventories after
hopping back to the main thread. GUIs never trust their own inventory
contents - every button carries a `PersistentDataContainer` action tag, all
clicks (including shift-click, drag, hotbar-swap, double-click) are
cancelled by a single global listener, and every mutating action is
re-validated server-side (privacy, block state, friend limits, cooldown)
regardless of what the client displayed.

## Troubleshooting

- **Nothing happens when I open `/social`** - check `nexora.friend.use` is
  granted and the plugin loaded without a database error in the console
  (`debug: true` in config.yml adds more logging).
- **MySQL connection fails on startup** - the plugin disables itself if the
  database can't be reached; check host/port/credentials and that the
  target database exists.
- **Friend limit doesn't match my rank** - limits are permission-based, not
  group-name based; make sure the rank has the right `nexora.friend.limit.*`
  node (the *highest* one it holds is used automatically).
- **Skins don't show on heads** - this uses the player's UUID via
  `Bukkit#createProfile`; texture resolution happens server-side/async and
  can take a moment for players who have never joined this server before.
