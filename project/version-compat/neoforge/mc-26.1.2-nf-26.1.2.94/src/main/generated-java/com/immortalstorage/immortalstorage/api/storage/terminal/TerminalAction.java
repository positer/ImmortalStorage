package com.immortalstorage.immortalstorage.api.storage.terminal;

/** Server-authoritative operation applied to a stable terminal entry. */
public enum TerminalAction {
    PICKUP_STACK,
    PICKUP_ONE,
    QUICK_MOVE_TO_PLAYER,
    INSERT_CARRIED,
    INSERT_ONE;

    public static TerminalAction byId(int id) {
        TerminalAction[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }
}
