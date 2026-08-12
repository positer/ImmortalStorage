package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelKey;

/**
 * The three interchangeable Xianeng crystal variants.  The electric variant
 * is deliberately unconditional: its FE ledger is part of ImmortalStorage's
 * own public API and must remain available with no optional mod installed.
 */
public enum CrystalKind {
    ELECTRIC("energy_crystal", "electric", null, ExternalResourceChannels.FE,
            "FE", "电力", "Electricity"),
    MANA("mana_crystal", "mana", "botania", ExternalResourceChannels.BOTANIA_MANA,
            "Mana", "魔力", "Mana"),
    SOURCE("source_crystal", "source", "ars_nouveau", ExternalResourceChannels.ARS_NOUVEAU_SOURCE,
            "Source", "魔源", "Source");

    private final String registryPath;
    private final String uiPath;
    private final String optionalModId;
    private final ResourceChannelKey channel;
    private final String unit;
    private final String chineseName;
    private final String englishName;

    CrystalKind(String registryPath, String uiPath, String optionalModId,
                ResourceChannelKey channel, String unit,
                String chineseName, String englishName) {
        this.registryPath = registryPath;
        this.uiPath = uiPath;
        this.optionalModId = optionalModId;
        this.channel = channel;
        this.unit = unit;
        this.chineseName = chineseName;
        this.englishName = englishName;
    }

    public String registryPath() {
        return registryPath;
    }

    public String uiPath() {
        return uiPath;
    }

    public String optionalModId() {
        return optionalModId;
    }

    public boolean unconditional() {
        return optionalModId == null;
    }

    public ResourceChannelKey channel() {
        return channel;
    }

    public String unit() {
        return unit;
    }

    public String chineseName() {
        return chineseName;
    }

    public String englishName() {
        return englishName;
    }

    public String blockTranslationKey() {
        return "block.immortalstorage." + registryPath;
    }

    public String containerTranslationKey() {
        return "container.immortalstorage." + registryPath;
    }

    public String uiTranslationKey(String suffix) {
        return containerTranslationKey() + "." + suffix;
    }

    public static CrystalKind fromRegistryPath(String path) {
        for (CrystalKind kind : values()) {
            if (kind.registryPath.equals(path)) return kind;
        }
        return ELECTRIC;
    }
}
