package com.immortalstorage.immortalstorage.block.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfacePassivePipeContractTest {
    @Test
    void passiveCapabilitiesIgnoreActiveModesButExtractionUsesSlotMasks() throws IOException {
        Path java = locateProject().resolve(Path.of("src", "main", "java", "com",
                "immortalstorage", "immortalstorage"));
        String entity = Files.readString(java.resolve(Path.of("block", "entity",
                "XianqiaoInterfaceBlockEntity.java")));
        String items = Files.readString(java.resolve(Path.of("block", "entity",
                "XianqiaoInterfaceSidedItemHandler.java")));
        String fluids = Files.readString(java.resolve(Path.of("block", "entity",
                "XianqiaoInterfaceSidedFluidHandler.java")));
        String mekanism = Files.readString(java.resolve(Path.of("compat", "mekanism",
                "MekanismCompat.java")));

        assertTrue(entity.contains("resolveExternalResourcePipeStore"));
        assertTrue(items.contains("delegate.isOutputFaceEnabled(slot, side)"));
        assertTrue(fluids.contains("resources.isOutputFaceEnabled(tank, side)"));
        assertFalse(items.contains("SideMode"));
        assertFalse(fluids.contains("SideMode"));
        assertTrue(mekanism.contains("resolveExternalResourcePipeStore"));
        assertTrue(mekanism.contains("&& blockEntity.isActivePushEnabled()"),
                "active Mekanism emission must still obey the global active-push switch");
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    && Files.isDirectory(current.resolve("src/main/java"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project root");
    }
}
