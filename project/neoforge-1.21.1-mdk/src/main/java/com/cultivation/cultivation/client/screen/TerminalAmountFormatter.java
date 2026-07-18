package com.cultivation.cultivation.client.screen;

/** AE-style compact formatter for terminal quantities that remain server-owned longs. */
public final class TerminalAmountFormatter {
    private static final String[] UNITS = {"", "k", "M", "G", "T", "P", "E"};

    public static String format(long amount) {
        long safeAmount = Math.max(0L, amount);
        if (safeAmount < 1_000L) return Long.toString(safeAmount);

        double scaled = safeAmount;
        int unit = 0;
        while (scaled >= 1_000.0D && unit < UNITS.length - 1) {
            scaled /= 1_000.0D;
            unit++;
        }
        if (scaled < 10.0D) {
            double truncated = Math.floor(scaled * 10.0D) / 10.0D;
            if (truncated != Math.floor(truncated)) {
                return String.format(java.util.Locale.ROOT, "%.1f%s", truncated, UNITS[unit]);
            }
        }
        return ((long) Math.floor(scaled)) + UNITS[unit];
    }

    private TerminalAmountFormatter() {}
}
