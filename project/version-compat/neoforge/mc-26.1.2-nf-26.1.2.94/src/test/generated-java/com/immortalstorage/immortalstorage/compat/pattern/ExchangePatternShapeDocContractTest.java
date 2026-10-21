package com.immortalstorage.immortalstorage.compat.pattern;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks {@code docs/0.1.9-exchange-pattern-shape.md} to the {@code javap} signatures it claims.
 *
 * <p>WHY: the document is the contract the AE2 / Refined Storage bridges are written against. Its
 * value comes entirely from the signatures being quoted verbatim from the four reference jars
 * (AE2 19.2.17, AE2 26.1.10-beta, RS 2.0.9, RS 3.2.1, class versions 65 / 69). If somebody edits a
 * signature in the document to match code instead of a jar, or drops the bytecode evidence for the
 * "a zero-input pattern is rejected" rule, this test fails and the edit has to be re-measured.</p>
 *
 * <p>This test is deliberately pure JUnit: it never loads an AE2 or Refined Storage class, because
 * the canonical test classpath does not guarantee they exist (the 1.21.1 lane is not the audited
 * matrix lane). Nothing here needs a Minecraft bootstrap either — it only reads text.</p>
 *
 * <p>The document lives at the workspace root ({@code <repo>/docs/}) in both lanes, so the lookup
 * walks up from {@code user.dir} instead of assuming the NeoForge project directory.</p>
 */
final class ExchangePatternShapeDocContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final String DOC_RELATIVE_PATH = "docs/0.1.9-exchange-pattern-shape.md";

    /** AE2 interface/API shapes the exchange pattern is built from. */
    private static final List<String> AE2_SIGNATURES = List.of(
            "public abstract appeng.api.stacks.AEItemKey getDefinition();",
            "public abstract appeng.api.crafting.IPatternDetails$IInput[] getInputs();",
            "public abstract java.util.List<appeng.api.stacks.GenericStack> getOutputs();",
            "public abstract appeng.api.stacks.GenericStack[] getPossibleInputs();",
            "public abstract long getMultiplier();",
            "public abstract appeng.api.stacks.AEKey getRemainingKey(appeng.api.stacks.AEKey);",
            "public abstract boolean pushPattern(appeng.api.crafting.IPatternDetails, appeng.api.stacks.KeyCounter[]);",
            "public static net.minecraft.world.item.ItemStack encodeProcessingPattern("
                    + "java.util.List<appeng.api.stacks.GenericStack>, java.util.List<appeng.api.stacks.GenericStack>);",
            "public static final int MAX_INPUT_SLOTS = 81;",
            "public static final int MAX_OUTPUT_SLOTS = 27;",
            "public appeng.helpers.patternprovider.PatternProviderLogic("
                    + "appeng.api.networking.IManagedGridNode, "
                    + "appeng.helpers.patternprovider.PatternProviderLogicHost);");

    /** Refined Storage shapes: records, the external layout factory, and both generations' sinks. */
    private static final List<String> RS_SIGNATURES = List.of(
            "public com.refinedmods.refinedstorage.api.autocrafting.Pattern("
                    + "java.util.UUID, com.refinedmods.refinedstorage.api.autocrafting.PatternLayout);",
            "public static com.refinedmods.refinedstorage.api.autocrafting.PatternLayout external("
                    + "java.util.List<com.refinedmods.refinedstorage.api.autocrafting.Ingredient>, "
                    + "java.util.List<com.refinedmods.refinedstorage.api.resource.ResourceAmount>);",
            "public com.refinedmods.refinedstorage.api.autocrafting.Ingredient(long, "
                    + "java.util.List<com.refinedmods.refinedstorage.api.resource.ResourceKey>);",
            "public com.refinedmods.refinedstorage.api.resource.ResourceAmount("
                    + "com.refinedmods.refinedstorage.api.resource.ResourceKey, long);",
            "public void setPattern(int, com.refinedmods.refinedstorage.api.autocrafting.Pattern);",
            "public void tryUpdatePattern(int, com.refinedmods.refinedstorage.api.autocrafting.Pattern);");

    /** Every rejection rule the doc claims, quoted from bytecode string constants. */
    private static final List<String> REJECTION_EVIDENCE = List.of(
            "At least one input must be non-null.",
            "The first (primary) output must be non-null.",
            "Ingredients cannot be empty",
            "Outputs cannot be empty",
            "External patterns cannot have byproducts",
            "Amount must be larger than zero",
            "Inputs cannot be empty");

    @Test
    void documentQuotesEveryAe2SignatureVerbatim() throws Exception {
        String document = readDocument();
        for (String signature : AE2_SIGNATURES) {
            assertTrue(document.contains(signature),
                    "docs/0.1.9-exchange-pattern-shape.md must quote the AE2 signature verbatim: " + signature);
        }
    }

    @Test
    void documentQuotesEveryRefinedStorageSignatureVerbatim() throws Exception {
        String document = readDocument();
        for (String signature : RS_SIGNATURES) {
            assertTrue(document.contains(signature),
                    "docs/0.1.9-exchange-pattern-shape.md must quote the RS signature verbatim: " + signature);
        }
    }

    @Test
    void documentKeepsTheZeroInputRejectionEvidence() throws Exception {
        String document = readDocument();
        for (String evidence : REJECTION_EVIDENCE) {
            assertTrue(document.contains(evidence),
                    "the bytecode rejection constant must stay quoted: " + evidence);
        }
        assertTrue(document.contains("免费源脉"),
                "the free-source-vein rule (a zero-input pattern must not be published) must stay documented");
        assertTrue(document.contains("Long.MAX_VALUE"),
                "the free vein infinite-availability fact is the reason zero-input patterns are refused");
    }

    @Test
    void documentStaysBoundToTheFourReferenceJarsAndItsUnverifiedList() throws Exception {
        String document = readDocument();
        for (String jar : List.of("appliedenergistics2-19.2.17.jar", "appliedenergistics2-26.1.10-beta.jar",
                "refinedstorage-neoforge-2.0.9.jar", "refinedstorage-neoforge-3.2.1.jar")) {
            assertTrue(document.contains(jar), "the contract must name the measured jar it came from: " + jar);
        }
        assertTrue(document.contains("未核实"),
                "the contract must keep its unverified-items section instead of presenting guesses as measured");
        assertTrue(document.length() > 8_000, "the contract must stay a report, not a stub");
    }

    @Test
    void thisTestStillLoadsNoAe2OrRefinedStorageClass() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("ExchangePatternShapeDocContractTest.class")) {
            assertNotNull(in, "the compiled contract test must be readable from the test classpath");
            String constantPool = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
            // Built at runtime so the needles themselves are not constants inside the class file.
            String appengPackage = String.join("/", "appeng", "api");
            String refinedStoragePackage = String.join("/", "com", "refinedmods");
            assertFalse(constantPool.contains(appengPackage),
                    "the contract test must not reference AE2 classes on the canonical test classpath");
            assertFalse(constantPool.contains(refinedStoragePackage),
                    "the contract test must not reference Refined Storage classes on the canonical test classpath");
        }
    }

    private static String readDocument() throws Exception {
        Path root = locateRepositoryRoot();
        Path document = root.resolve(DOC_RELATIVE_PATH);
        assertTrue(Files.isRegularFile(document), "missing contract document: " + document);
        return Files.readString(document);
    }

    private static Path locateRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve(DOC_RELATIVE_PATH))
                    && Files.isRegularFile(current.resolve("CHANGELOG.md"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException(
                "cannot locate the canonical test root; neither " + DOC_RELATIVE_PATH
                        + " nor CHANGELOG.md was found above " + Path.of("").toAbsolutePath());
    }
}
