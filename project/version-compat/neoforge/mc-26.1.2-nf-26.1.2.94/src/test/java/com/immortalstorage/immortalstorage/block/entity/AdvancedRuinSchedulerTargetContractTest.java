package com.immortalstorage.immortalstorage.block.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Target-side source contract proving the shared scheduler fixes survive generation. */
@ExtendWith(com.immortalstorage.immortalstorage.compat.CompatTestBootstrapExtension.class)
final class AdvancedRuinSchedulerTargetContractTest {
    @Test
    void generatedSchedulerRetainsFilterAndAllSlotInsertionLogic() throws IOException {
        Path relative = Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", "generated-java",
                "com", "immortalstorage", "immortalstorage", "block", "entity",
                "AdvancedRuinScheduler.java");
        Path generated = Path.of("").toAbsolutePath();
        while (generated != null && !Files.exists(generated.resolve(relative))) {
            generated = generated.getParent();
        }
        assertTrue(generated != null, "workspace root for generated compatibility source is not visible");
        generated = generated.resolve(relative);
        String source = Files.readString(generated);
        assertTrue(source.contains("allows.test(stack)"));
        assertTrue(source.contains("private static ItemStack insertInto"));
    }
}
