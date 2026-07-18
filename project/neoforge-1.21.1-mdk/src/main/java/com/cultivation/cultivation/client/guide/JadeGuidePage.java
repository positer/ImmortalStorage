package com.cultivation.cultivation.client.guide;

import java.util.List;

public record JadeGuidePage(String title, List<String> lines) {
    public JadeGuidePage {
        lines = List.copyOf(lines);
    }
}
