package com.immortalstorage.immortalstorage.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ImmortalStorageClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue TERMINAL_ROWS = BUILDER
            .translation(key("terminalRows"))
            .comment("Preferred storage-terminal rows. The screen clamps this to the available height.")
            .defineInRange("terminalRows", 5, 2, 12);

    public static final ModConfigSpec.BooleanValue SYNC_RECIPE_VIEWER_SEARCH = BUILDER
            .translation(key("syncRecipeViewerSearch"))
            .comment("Synchronize terminal search with installed JEI or EMI search fields.")
            .define("syncRecipeViewerSearch", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ImmortalStorageClientConfig() {}

    private static String key(String path) {
        return "immortalstorage.configuration." + path;
    }
}
