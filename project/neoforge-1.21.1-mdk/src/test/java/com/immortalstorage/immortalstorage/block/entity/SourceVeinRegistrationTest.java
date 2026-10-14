package com.immortalstorage.immortalstorage.block.entity;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** A missing supported block crashes construction on 26.1.2, before rendering. */
final class SourceVeinRegistrationTest {
    @Test void everyRegisteredSourceBlockIsAcceptedByItsBlockEntityType() throws Exception {
        Path root = locateMainSources();
        String blocks = Files.readString(root.resolve("block/ModBlocks.java"));
        String entities = Files.readString(root.resolve("block/entity/ModBlockEntities.java"));
        var declarations = Pattern.compile("Supplier<Block>\\s+(\\w+)\\s*=\\s*reg\\([^;]+new SourceVeinBlock\\(").matcher(blocks);
        Set<String> sources = new HashSet<>();
        while (declarations.find()) sources.add(declarations.group(1));
        assertTrue(sources.contains("ECHO_SHARD_VEIN"));
        assertTrue(sources.contains("CUSTOM_SOURCE_VEIN"));
        String registration = entities.substring(entities.indexOf("SOURCE_VEIN ="), entities.indexOf("XIANQIAO_MANAGER ="));
        var supported = Pattern.compile("ModBlocks\\.(\\w+)\\.get\\(\\)").matcher(registration);
        Set<String> accepted = new HashSet<>();
        while (supported.find()) accepted.add(supported.group(1));
        assertEquals(sources, accepted, "Every source placement must pass BlockEntityType state validation");
    }
    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "java", "com", "immortalstorage", "immortalstorage"));
            if (Files.isRegularFile(candidate.resolve("block/ModBlocks.java"))) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate main sources");
    }
}
