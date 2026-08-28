package com.immortalstorage.immortalstorage.block;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class XianqiaoRedstoneInterfaceResourceContractTest {
    @Test void blockstateMapsBothActivationTexturesAndMachineTagsUseRegisteredIds() throws Exception {
        Path resources = locateMain().resolve("resources");
        String state = Files.readString(resources.resolve("assets/immortalstorage/blockstates/xianqiao_redstone_interface.json"));
        assertTrue(state.contains("activated=false"));
        assertTrue(state.contains("activated=true"));
        assertTrue(state.contains("xianqiao_redstone_interface_inactivated"));
        assertTrue(state.contains("xianqiao_redstone_interface_activated"));
        assertTrue(Files.size(resources.resolve("assets/immortalstorage/textures/block/xianqiao_redstone_interface_inactivated.png")) > 0);
        assertTrue(Files.size(resources.resolve("assets/immortalstorage/textures/block/xianqiao_redstone_interface_activated.png")) > 0);
        for (String stateName : new String[]{"inactivated", "activated"}) {
            String model = Files.readString(resources.resolve(
                    "assets/immortalstorage/models/block/xianqiao_redstone_interface_" + stateName + ".json"));
            assertFalse(model.contains("\"texture_size\""), stateName);
            assertTrue(model.contains("\"parent\":\"minecraft:block/block\""), stateName);
            assertTrue(model.contains("\"uv\":[12,0,8,4]"), stateName);
            assertFalse(model.contains("\"uv\":[48,0,32,16]"), stateName);
        }
        assertEquals("2B199C4A7E9B59E4CE9997393E320CDE3A0DFDA8C9C903D5C8DACB7680B399A5",
                sha256(resources.resolve("assets/immortalstorage/textures/block/xianqiao_redstone_interface_inactivated.png")));
        assertEquals("A83D401E8ECB75500F7DDE828E80F76B00E86CCFF821A5EF72C7BD929E8976CB",
                sha256(resources.resolve("assets/immortalstorage/textures/block/xianqiao_redstone_interface_activated.png")));
        assertTrue(Files.isRegularFile(resources.resolve(
                "assets/immortalstorage/models/item/xianqiao_redstone_interface.json")));
        var storage = JsonParser.parseString(Files.readString(resources.resolve("data/immortalstorage/tags/blocks/storage_machines.json"))).getAsJsonObject();
        assertTrue(storage.getAsJsonArray("values").toString().contains("immortalstorage:miniature_immortal_ruin"));
        assertFalse(storage.getAsJsonArray("values").toString().contains("miniature_immortal_ruin_block"));
    }

    @Test void redstoneSourceDoesNotExposeMachineInputRedstoneMode() throws Exception {
        Path main = locateMain();
        String menu = Files.readString(main.resolve(
                "java/com/immortalstorage/immortalstorage/menu/custom/XianqiaoRedstoneInterfaceMenu.java"));
        String screen = Files.readString(main.resolve(
                "java/com/immortalstorage/immortalstorage/client/screen/XianqiaoRedstoneInterfaceScreen.java"));
        assertFalse(menu.contains("implements MachineRedstoneMenu"));
        assertFalse(screen.contains("MachineRedstoneModeButton"));
        assertTrue(menu.contains("CONFIGURATION_SYNC_MARKER"));
        String entity = Files.readString(main.resolve(
                "java/com/immortalstorage/immortalstorage/block/entity/XianqiaoRedstoneInterfaceBlockEntity.java"));
        assertTrue(entity.contains("tag.putLong(\"LowThreshold\", lowThreshold)"));
        assertTrue(entity.contains("tag.putLong(\"HighThreshold\", highThreshold)"));
        assertTrue(entity.contains("evaluate(configuredOwnerStorageAmount(0))"));
    }

    private static Path locateMain() {
        for (Path cursor = Path.of("").toAbsolutePath(); cursor != null; cursor = cursor.getParent()) {
            Path workspace = cursor.resolve("project/neoforge-1.21.1-mdk/src/main");
            if (Files.isDirectory(workspace)) return workspace;
            Path module = cursor.resolve("src/main");
            if (Files.isDirectory(module)) return module;
        }
        throw new IllegalStateException("Could not locate main resource root");
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
