package com.cultivation.cultivation.client.screen;

import net.neoforged.neoforge.fluids.FluidType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Compact AE-style bucket counts for a 16 px terminal cell. */
public final class TerminalFluidAmountFormatter {
    private static final String[] SUFFIXES = {"B", "kB", "MB", "GB", "TB", "PB", "EB"};

    public static String format(long amountMb) {
        if (amountMb <= 0L) return "0B";
        BigDecimal value = BigDecimal.valueOf(amountMb)
                .divide(BigDecimal.valueOf(FluidType.BUCKET_VOLUME), 6, RoundingMode.DOWN);
        int suffix = 0;
        while (value.compareTo(BigDecimal.valueOf(1000L)) >= 0 && suffix < SUFFIXES.length - 1) {
            value = value.divide(BigDecimal.valueOf(1000L), 6, RoundingMode.DOWN);
            suffix++;
        }
        int decimals = value.compareTo(BigDecimal.valueOf(100L)) >= 0 ? 0
                : value.compareTo(BigDecimal.TEN) >= 0 ? 1
                : value.compareTo(BigDecimal.ONE) >= 0 ? 2 : 3;
        return value.setScale(decimals, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
                + SUFFIXES[suffix];
    }

    public static String exactBuckets(long amountMb) {
        if (amountMb <= 0L) return "0 B";
        return BigDecimal.valueOf(amountMb)
                .divide(BigDecimal.valueOf(FluidType.BUCKET_VOLUME), 6, RoundingMode.DOWN)
                .stripTrailingZeros().toPlainString() + " B";
    }

    public static String exactMillibuckets(long amountMb) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.ROOT);
        format.setGroupingUsed(true);
        return format.format(Math.max(0L, amountMb)) + " mB";
    }

    private TerminalFluidAmountFormatter() {}
}
