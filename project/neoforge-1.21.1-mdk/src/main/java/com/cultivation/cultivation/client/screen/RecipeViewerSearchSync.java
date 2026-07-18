package com.cultivation.cultivation.client.screen;

import com.cultivation.cultivation.config.CultivationClientConfig;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

/** Keeps optional recipe-viewer APIs out of the terminal screen's class linkage. */
final class RecipeViewerSearchSync {
    private static String lastPushed = "";
    private static Source lastSource = Source.NONE;
    private static long lastPushNanos;
    private static String pendingExternal;

    private RecipeViewerSearchSync() {}

    static void push(String text) {
        if (!CultivationClientConfig.SYNC_RECIPE_VIEWER_SEARCH.get()) {
            return;
        }
        String normalized = text == null ? "" : text;
        lastPushed = normalized;
        lastSource = Source.TERMINAL;
        lastPushNanos = System.nanoTime();
        if (ModList.get().isLoaded("jei")) {
            invokeStatic("com.cultivation.cultivation.compat.jei.CultivationJeiPlugin",
                    "setSearchText", new Class<?>[]{String.class}, normalized);
        }
        if (ModList.get().isLoaded("emi")) {
            invokeStatic("dev.emi.emi.api.EmiApi", "setSearchText", new Class<?>[]{String.class}, normalized);
        }
    }

    static Optional<String> externalText() {
        if (!CultivationClientConfig.SYNC_RECIPE_VIEWER_SEARCH.get()) {
            return Optional.empty();
        }
        if (ModList.get().isLoaded("emi")) {
            Object focused = invokeStatic("dev.emi.emi.api.EmiApi", "isSearchFocused", new Class<?>[0]);
            if (Boolean.TRUE.equals(focused)) {
                Object value = invokeStatic("dev.emi.emi.api.EmiApi", "getSearchText", new Class<?>[0]);
                if (value instanceof String text) {
                    return externalValue(Source.EMI, text);
                }
            }
        }
        if (ModList.get().isLoaded("jei")) {
            Object focused = invokeStatic("com.cultivation.cultivation.compat.jei.CultivationJeiPlugin",
                    "isSearchFocused", new Class<?>[0]);
            if (Boolean.TRUE.equals(focused)) {
                Object value = invokeStatic("com.cultivation.cultivation.compat.jei.CultivationJeiPlugin",
                        "getSearchText", new Class<?>[0]);
                if (value instanceof String text) {
                    return externalValue(Source.JEI, text);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> externalValue(Source source, String text) {
        String normalized = text == null ? "" : text;
        long now = System.nanoTime();
        if (lastSource == Source.TERMINAL && normalized.equals(lastPushed) && now - lastPushNanos <= 250_000_000L) {
            return Optional.empty();
        }
        if (normalized.equals(pendingExternal) && now - lastPushNanos <= 100_000_000L) return Optional.empty();
        lastSource = source;
        lastPushed = normalized;
        pendingExternal = normalized;
        lastPushNanos = now;
        return Optional.of(normalized);
    }

    private enum Source { NONE, TERMINAL, JEI, EMI }

    private static Object invokeStatic(String className, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> type = Class.forName(className, false, RecipeViewerSearchSync.class.getClassLoader());
            Method method = type.getMethod(methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
