package com.immortalstorage.immortalstorage.locator;

public record ToolLocatorWindow(int x, int z) {
    public boolean containsChunk(int chunkX, int chunkZ) {
        return Math.abs((long) chunkX - x) <= ToolLocatorSettings.CACHE_CHUNKS
                && Math.abs((long) chunkZ - z) <= ToolLocatorSettings.CACHE_CHUNKS;
    }
}
