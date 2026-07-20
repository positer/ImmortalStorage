package com.immortalstorage.immortalstorage.compat.arsnouveau;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class RelayTileXianqiaoLoopContractTest {
    @Test
    void oneInterfaceCannotRemainBothRelayEndpoints() throws IOException {
        Path source = locateProject().resolve(Path.of(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "mixin", "arsnouveau", "RelayTileXianqiaoMixin.java"));
        String mixin = Files.readString(source);

        assertTrue(mixin.contains("if (storedPos.equals(this.toPos))"));
        assertTrue(mixin.contains("if (pos.equals(this.fromPos)) this.fromPos = null"));
        assertTrue(mixin.contains("if (fromPos != null && fromPos.equals(toPos))"));
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
