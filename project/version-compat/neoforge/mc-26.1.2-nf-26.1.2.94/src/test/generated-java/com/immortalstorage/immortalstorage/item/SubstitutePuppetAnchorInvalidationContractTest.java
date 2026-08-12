package com.immortalstorage.immortalstorage.item;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SubstitutePuppetAnchorInvalidationContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void brokenRespawnAnchorClearsPuppetsAcrossPlayerAndPersonalStorage() throws Exception {
        Path java = locateMainJava();
        String item = Files.readString(java.resolve("item/custom/SubstitutePuppetItem.java"));
        String events = Files.readString(java.resolve("event/CommonEvents.java"));

        assertTrue(item.contains("clearAnchorIfMatches(ItemStack stack"));
        assertTrue(item.contains("clearInvalidAnchor(ServerPlayer player, ItemStack stack)"));
        assertTrue(item.contains("targetLevel.getBlockState"));
        assertTrue(item.contains("RealmHelper"));
        assertTrue(item.contains("resolveOwnedPersonalRealm(player, key)"));
        assertTrue(item.contains("clearInvalidAnchor(player, stack)"));
        assertTrue(events.contains("onRespawnAnchorBroken(BreakBlockEvent event)"));
        assertTrue(events.contains("EventPriority.LOWEST"));
        assertTrue(events.contains("event.isCanceled()"));
        assertTrue(events.contains("data.getKongqiaoItems()"));
        assertTrue(events.contains("data.getXianqiaoStorageItems()"));
        assertTrue(events.contains("data.setXianqiaoSlot(slot, stack)"));
        assertTrue(events.contains("clearInvalidPuppetAnchors(p, data)"));
        assertTrue(events.contains("for (BlockPos pos : e.getAffectedBlocks())"));
        assertTrue(events.indexOf("teleportToAnchor(p, puppet)") < events.indexOf("puppetLocation.commit().run()"));
    }

    private static Path locateMainJava() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com",
                    "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate main sources");
    }
}
