package com.immortalstorage.immortalstorage;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ZeroPointZeroPointFourContractTest {
    private static final Path ROOT = locateProject();

    @Test void primordialQiUsesRegistryEggsDragonCompletionAndTribulationGuard() throws Exception {
        String conversion = source("entity/PrimordialQiConversion.java");
        String item = source("item/custom/PrimordialQiItem.java");
        assertTrue(conversion.contains("BuiltInRegistries.ITEM.stream()"));
        assertTrue(conversion.contains("DURATION = 10"));
        assertTrue(conversion.contains("item instanceof SpawnEggItem"));
        assertTrue(conversion.contains("candidate.getType(candidate.getDefaultInstance()) == entityType"));
        assertTrue(conversion.contains("fight.setDragonKilled(dragon)"));
        assertTrue(conversion.contains("living.discard()"));
        assertTrue(item.contains("isTribulationActive()"));
        assertTrue(item.contains("PRIMORDIAL_QI_ENTITY_BLACKLIST"));
    }

    @Test void newItemsRecipesAndTexturesAreComplete() throws Exception {
        String items = source("item/ModItems.java");
        assertTrue(items.contains("durability(1024)"));
        assertTrue(items.contains("stacksTo(16)"));
        assertTrue(items.contains("PRIMORDIAL_QI"));
        for (String name : new String[]{"qi_collecting_bottle", "disposable_qi_collecting_bottle"}) {
            assertTrue(Files.isRegularFile(ROOT.resolve("src/main/resources/data/immortalstorage/recipe/" + name + ".json")));
        }
        for (String name : new String[]{"qi_collecting_bottle", "disposable_qi_collecting_bottle", "primordial_qi", "spirit_staff_teleport"}) {
            assertTrue(Files.isRegularFile(ROOT.resolve("src/main/resources/assets/immortalstorage/textures/item/" + name + ".png")));
        }
        assertTrue(Files.isRegularFile(ROOT.resolve("src/main/resources/assets/immortalstorage/textures/gui/external_resource/soul_surge.png")));
    }

    @Test void archaeologyCoversEveryVanillaSuspiciousBlockTable() throws Exception {
        var json = JsonParser.parseString(Files.readString(ROOT.resolve(
                "src/main/resources/data/immortalstorage/loot_modifiers/jade_guide_from_archaeology.json"))).getAsJsonObject();
        assertEquals("immortalstorage:archaeology_jade", json.get("type").getAsString());
        String text = json.toString();
        for (String table : new String[]{"desert_pyramid", "desert_well", "ocean_ruin_cold", "ocean_ruin_warm", "trail_ruins_common", "trail_ruins_rare"}) {
            assertTrue(text.contains(table));
        }
        assertTrue(Files.readString(ROOT.resolve("src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers.json"))
                .contains("jade_guide_from_archaeology"));
    }

    @Test void terminalSmithingAndViewerTransferAreWired() throws Exception {
        assertTrue(source("menu/custom/KongqiaoMenu.java").contains("isSmithingUnlocked() { return data.getStage() >= 4; }"));
        assertTrue(source("menu/custom/XianqiaoStorageMenu.java").contains("SmithingTransferTarget"));
        assertTrue(source("compat/jei/ImmortalStorageJeiPlugin.java").contains("RecipeTypes.SMITHING"));
        assertTrue(source("compat/emi/ImmortalStorageEmiPlugin.java").contains("VanillaEmiRecipeCategories.SMITHING"));
    }

    @Test void ruinFiltersLinksWarpAndAbsoluteRestraintArePersistent() throws Exception {
        String stable = source("block/entity/StabilizedMiniatureImmortalRuinBlockEntity.java");
        String mini = source("block/entity/MiniatureImmortalRuinBlockEntity.java");
        assertTrue(stable.contains("NonNullList.withSize(20"));
        assertTrue(stable.contains("FilterWhitelist"));
        assertTrue(stable.contains("FilterMatchComponents"));
        assertTrue(stable.contains("linkWith("));
        assertTrue(mini.contains("WarpEnabled"));
        assertTrue(mini.contains("warpLinkedEntities"));
        assertTrue(mini.contains("entity.getBoundingBox().intersects(centerArea)"));
        assertTrue(mini.contains("warpLinkedEntities(serverLevel, centerArea)"));
        assertTrue(mini.contains("MiniatureImmortalRuinEffectPolicy.effectArea(worldPosition)"));
        assertTrue(mini.contains("canAffect(entity)"));
        assertTrue(mini.contains("getMainHandItem()"));
        assertTrue(mini.contains("getOffhandItem()"));
        assertFalse(mini.contains("affectPlayers || !(entity instanceof Player)"));
        assertTrue(source("entity/AbsoluteRestraint.java").contains("entity.noPhysics = true"));
    }

    @Test void spiritStaffTeleportIsExactAndIgnoresAllCollision() throws Exception {
        String staff = source("item/custom/SpiritStaffItem.java");
        assertTrue(staff.contains("start.add(look.scale(requested))"));
        assertFalse(staff.contains("level.noCollision(serverPlayer"));
    }

    @Test void realmManagementPanelContainsMagnetAndEnvironmentControls() throws Exception {
        String screen = source("client/screen/XianqiaoStorageScreen.java");
        assertTrue(screen.contains("REALM_HEIGHT = 160"));
        assertTrue(screen.contains("REALM_WIDTH, REALM_HEIGHT"));
        assertTrue(screen.contains("this.magnetButton.setMessage(magnetLabel())"));
        assertTrue(screen.contains("this.dayNightButton") && screen.contains("this.weatherButton"));
        assertTrue(screen.contains("new ModPayloads.RealmEnvironment(this.menu.containerId, 0)"));
        assertTrue(screen.contains("new ModPayloads.RealmEnvironment(this.menu.containerId, 1)"));
        int craft = screen.indexOf("new ItemStack(Items.CRAFTING_TABLE)");
        int smithing = screen.indexOf("new ItemStack(Items.SMITHING_TABLE)");
        int furnace = screen.indexOf("new ItemStack(ModBlocks.IMMORTAL_FURNACE.get())");
        int realm = screen.indexOf("new ItemStack(Items.ENDER_EYE)");
        assertTrue(craft < smithing && smithing < furnace && furnace < realm);
    }

    private static String source(String relative) throws Exception {
        return Files.readString(ROOT.resolve("src/main/java/com/immortalstorage/immortalstorage").resolve(relative));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("gradle.properties")) && Files.isDirectory(current.resolve("src/main"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate project");
    }
}
