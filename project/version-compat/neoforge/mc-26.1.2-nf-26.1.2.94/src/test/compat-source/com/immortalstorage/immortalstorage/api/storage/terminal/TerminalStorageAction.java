package com.immortalstorage.immortalstorage.api.storage.terminal;

/** Mutation mode shared by terminal-native long-amount storage contracts. */
public enum TerminalStorageAction {
    SIMULATE,
    EXECUTE;

    public boolean executes() {
        return this == EXECUTE;
    }
}
