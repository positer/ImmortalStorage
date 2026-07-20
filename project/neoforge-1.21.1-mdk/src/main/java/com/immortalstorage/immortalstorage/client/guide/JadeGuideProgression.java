package com.immortalstorage.immortalstorage.client.guide;

public final class JadeGuideProgression {
    private JadeGuideProgression() {
    }

    public static String summaryKey(int stage) {
        return "guide.immortalstorage.jade.stage." + Math.max(0, Math.min(10, stage)) + ".summary";
    }

    public static String nextGoalKey(int stage, boolean stageTenInfiniteImmortalYuan) {
        int bounded = Math.max(0, Math.min(10, stage));
        if (bounded == 10) {
            return "guide.immortalstorage.jade.stage.10.next."
                    + (stageTenInfiniteImmortalYuan ? "infinite" : "generated");
        }
        return "guide.immortalstorage.jade.stage." + bounded + ".next";
    }
}
