package com.immortalstorage.immortalstorage.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Immortal Artifact's mode textures on both renderer lanes.
 *
 * <p>1.21.1 resolves the per-mode texture through legacy model overrides and
 * two float item predicates; 26.1 replaced both with an item definition driven
 * by named select properties. Real-client QA found the artifact falling back to
 * its base sprite on 26.1.2 because only the Spirit Instrument definition had
 * been migrated, so both lanes are asserted here.</p>
 */
final class ImmortalArtifactModeResourceTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final List<String> MODES = List.of(
            "explore", "wrench", "pick", "build", "teleport");
    private static final List<String> TOOLS = List.of(
            "pick", "axe", "shovel", "hoe", "shears");

    @Test
    void legacyLaneKeepsModelOverridesAndBothModePredicates() throws IOException {
        Path project = locateProject();
        Path models = project.resolve("src/main/resources/assets/immortalstorage/models/item");
        String artifact = Files.readString(models.resolve("immortal_artifact.json"));
        for (int mode = 1; mode <= 4; mode++) {
            assertTrue(artifact.contains("\"immortalstorage:staff_mode\":" + mode),
                    "missing legacy mode override " + mode);
        }
        for (String tool : TOOLS) {
            assertTrue(Files.isRegularFile(models.resolve("immortal_artifact_" + tool + ".json")),
                    "missing legacy tool model " + tool);
        }
        assertTrue(artifact.contains("\"immortalstorage:artifact_tool\":1"));
        assertTrue(artifact.contains("\"immortalstorage:artifact_tool\":4"));

        Path textures = project.resolve("src/main/resources/assets/immortalstorage/textures/item");
        for (String mode : MODES) {
            assertTrue(Files.isRegularFile(textures.resolve("immortal_artifact_" + mode + ".png")),
                    "missing legacy mode texture " + mode);
        }

        String clientSetup = Files.readString(project.resolve(Path.of(
                "src/main/java/com/immortalstorage/immortalstorage/client/ClientSetup.java")));
        assertTrue(clientSetup.contains("ModItems.IMMORTAL_ARTIFACT.get()"));
        assertTrue(clientSetup.contains("\"staff_mode\""));
        assertTrue(clientSetup.contains("\"artifact_tool\""));
        assertTrue(clientSetup.contains("SpiritStaffItem.getMode(stack)"));
        assertTrue(clientSetup.contains("SpiritStaffItem.miningVisual("));
    }

    @Test
    void targetLaneDispatchesTheArtifactThroughBothSelectProperties() throws IOException {
        Path workspace = locateWorkspace();
        Path target = workspace.resolve(Path.of(
                "project", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94"));
        String definition = Files.readString(target.resolve(Path.of(
                "src", "main", "resources", "assets", "immortalstorage", "items", "immortal_artifact.json")));

        assertTrue(definition.contains("\"property\": \"immortalstorage:staff_mode\""),
                "artifact definition must select the staff mode property");
        assertTrue(definition.contains("\"immortalstorage:artifact_tool\""),
                "the pick case must nest the adaptive tool selection");
        for (String mode : List.of("wrench", "pick", "build", "teleport")) {
            assertTrue(definition.contains("\"when\": \"" + mode + "\""),
                    "missing artifact mode case " + mode);
            assertTrue(definition.contains("immortalstorage:item/immortal_artifact_" + mode),
                    "missing artifact mode model " + mode);
        }
        for (String tool : List.of("axe", "shovel", "hoe", "shears")) {
            assertTrue(definition.contains("\"when\": \"" + tool + "\""),
                    "missing artifact tool case " + tool);
        }
        assertTrue(definition.contains("immortalstorage:item/immortal_artifact_explore"),
                "the fallback must stay the explore sprite");

        // Keep the override source path in one literal: the 26.1 generator
        // repoints segmented "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source" test paths at its audit tree,
        // which intentionally contains only the merged canonical sources.
        String toolProperty = Files.readString(target.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/client/ImmortalArtifactToolProperty.java"));
        assertTrue(toolProperty.contains("SelectItemModelProperty<String>"));
        assertTrue(toolProperty.contains("SpiritStaffItem.miningVisual("));
        assertTrue(toolProperty.contains("IShearable"));

        String generatedSetup = Files.readString(target.resolve(Path.of(
                "src", "main", "generated-java", "com", "immortalstorage", "immortalstorage",
                "client", "ClientSetup.java")));
        assertTrue(generatedSetup.contains("\"staff_mode\""));
        assertTrue(generatedSetup.contains("SpiritStaffModeProperty.TYPE"));
        assertTrue(generatedSetup.contains("\"artifact_tool\""));
        assertTrue(generatedSetup.contains("ImmortalArtifactToolProperty.TYPE"));

        String generator = Files.readString(workspace.resolve(Path.of(
                "project", "version-compat", "generate-compat-source.ps1")));
        assertTrue(generator.contains("'immortal_artifact'"),
                "the generator must keep the hand-written artifact definition");
        assertTrue(generator.contains("addTicketWithRadius(TicketType.PORTAL, landingChunk, 2)"),
                "the generator must keep the 26.1 landing-chunk ticket mapping");
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project");
    }

    private static Path locateWorkspace() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("project/version-compat"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate workspace root");
    }
}
