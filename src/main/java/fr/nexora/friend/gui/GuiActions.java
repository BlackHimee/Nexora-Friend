package fr.nexora.friend.gui;

/** PersistentDataContainer action identifiers used to route GUI clicks. */
public final class GuiActions {

    private GuiActions() {
    }

    public static final String NAV_BACK = "nav_back";
    public static final String NAV_HOME = "nav_home";
    public static final String NAV_CLOSE = "nav_close";
    public static final String NAV_PREV_PAGE = "nav_prev_page";
    public static final String NAV_NEXT_PAGE = "nav_next_page";

    public static final String OPEN_FRIENDS = "open_friends";
    public static final String OPEN_REQUESTS = "open_requests";
    public static final String OPEN_RECEIVED_REQUESTS = "open_received_requests";
    public static final String OPEN_SENT_REQUESTS = "open_sent_requests";
    public static final String OPEN_ONLINE = "open_online";
    public static final String OPEN_SEARCH = "open_search";
    public static final String OPEN_SELF_PROFILE = "open_self_profile";
    public static final String OPEN_SETTINGS = "open_settings";
    public static final String OPEN_BLOCKED = "open_blocked";

    public static final String PLAYER_HEAD = "player_head";
    public static final String INCOMING_REQUEST_HEAD = "incoming_request_head";
    public static final String OUTGOING_REQUEST_HEAD = "outgoing_request_head";
    public static final String BLOCKED_HEAD = "blocked_head";

    public static final String FRIEND_ADD = "friend_add";
    public static final String FRIEND_REMOVE = "friend_remove";
    public static final String FRIEND_CANCEL_REQUEST = "friend_cancel_request";
    public static final String FRIEND_ACCEPT_REQUEST = "friend_accept_request";
    public static final String FRIEND_DENY_REQUEST = "friend_deny_request";
    public static final String FRIEND_BLOCK = "friend_block";
    public static final String FRIEND_UNBLOCK = "friend_unblock";

    public static final String CONFIRM_YES = "confirm_yes";
    public static final String CONFIRM_NO = "confirm_no";

    public static final String SETTINGS_TOGGLE_NOTIFICATIONS = "settings_toggle_notifications";
    public static final String SETTINGS_TOGGLE_ONLINE = "settings_toggle_online";
    public static final String SETTINGS_TOGGLE_OFFLINE = "settings_toggle_offline";
    public static final String SETTINGS_TOGGLE_REQUESTS = "settings_toggle_requests";
    public static final String SETTINGS_CYCLE_ADD_PRIVACY = "settings_cycle_add_privacy";
    public static final String SETTINGS_CYCLE_PROFILE_PRIVACY = "settings_cycle_profile_privacy";

    public static final String NEW_SEARCH = "new_search";
    public static final String NOOP = "noop";
}
