package com.immortalstorage.immortalstorage.compat.mekanism;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runtime-only description of the official Mekanism API boundary.
 *
 * <p>This class intentionally contains no Mekanism type in a field or method
 * signature. The optional bootstrap may load it in an absent-Mekanism client,
 * dedicated server or data run without resolving third-party classes.</p>
 */
public final class MekanismApiDescriptor {
    /** Latest official 1.21.1 artifact in ModMaven metadata on 2026-07-15. */
    public static final String COMPILE_API_VERSION = "1.21.1-10.7.19.85";
    public static final String SUPPORTED_VERSION_RANGE = "[10.7.19,10.8)";
    public static final int MIN_XIANQIAO_STAGE = 8;

    /** IDs declared by Mekanism's official common Capabilities source. */
    public static final String CHEMICAL_CAPABILITY_ID = "mekanism:chemical_handler";
    public static final String STRICT_ENERGY_CAPABILITY_ID = "mekanism:strict_energy_handler";

    /** Public interfaces present in the official API classifier JAR. */
    public static final String CHEMICAL_HANDLER_CLASS =
            "mekanism.api.chemical.IChemicalHandler";
    public static final String STRICT_ENERGY_HANDLER_CLASS =
            "mekanism.api.energy.IStrictEnergyHandler";
    public static final String CHEMICAL_STACK_CLASS =
            "mekanism.api.chemical.ChemicalStack";
    public static final String ACTION_CLASS = "mekanism.api.Action";

    private static final List<String> REQUIRED_CLASSES = List.of(
            CHEMICAL_HANDLER_CLASS,
            STRICT_ENERGY_HANDLER_CLASS,
            CHEMICAL_STACK_CLASS,
            ACTION_CLASS);

    private MekanismApiDescriptor() {
    }

    public static Probe probe(ClassLoader loader) {
        Objects.requireNonNull(loader, "loader");
        List<String> missing = new ArrayList<>();
        List<String> linkageProblems = new ArrayList<>();
        for (String required : REQUIRED_CLASSES) {
            try {
                Class.forName(required, false, loader);
            } catch (ClassNotFoundException exception) {
                missing.add(required);
            } catch (LinkageError error) {
                linkageProblems.add(required + ": " + error.getClass().getSimpleName());
            }
        }
        return new Probe(List.copyOf(missing), List.copyOf(linkageProblems));
    }

    public record Probe(List<String> missingClasses, List<String> linkageProblems) {
        public Probe {
            missingClasses = List.copyOf(missingClasses);
            linkageProblems = List.copyOf(linkageProblems);
        }

        public boolean compatible() {
            return missingClasses.isEmpty() && linkageProblems.isEmpty();
        }
    }
}
