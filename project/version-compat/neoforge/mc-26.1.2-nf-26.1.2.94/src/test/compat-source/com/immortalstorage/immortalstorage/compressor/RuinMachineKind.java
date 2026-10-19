package com.immortalstorage.immortalstorage.compressor;

/**
 * The block above a miniature-ruin core decides which machine the shared 3x3x3 shell runs.
 * The kind is derived from the world every time it is read and is never stored in NBT.
 */
public enum RuinMachineKind {
    SPIRIT("immortalstorage:treasure_basin",
            "container.immortalstorage.spirit_compressor", "container.immortalstorage.spirit_decompressor",
            new ParticleColour(.76F, .97F, 1F), 0xC2F7FF),
    ENCHANTMENT("minecraft:enchanting_table",
            "container.immortalstorage.enchantment_compressor", "container.immortalstorage.enchantment_decompressor",
            new ParticleColour(0x80 / 255F, 1F, 0x20 / 255F), 0xE8FFB0);

    /** Client-local chain-particle colour; the spirit values are the ones shipped in 0.1.6. */
    public record ParticleColour(float red, float green, float blue) {}

    private final String coreBlockId;
    private final String compressorTitleKey;
    private final String decompressorTitleKey;
    private final ParticleColour particleColour;
    private final int particleFadeColour;

    RuinMachineKind(String coreBlockId, String compressorTitleKey, String decompressorTitleKey,
                    ParticleColour particleColour, int particleFadeColour) {
        this.coreBlockId = coreBlockId;
        this.compressorTitleKey = compressorTitleKey;
        this.decompressorTitleKey = decompressorTitleKey;
        this.particleColour = particleColour;
        this.particleFadeColour = particleFadeColour;
    }

    public String coreBlockId() { return coreBlockId; }
    public String titleKey(boolean reversed) { return reversed ? decompressorTitleKey : compressorTitleKey; }
    public ParticleColour particleColour() { return particleColour; }
    public int particleFadeColour() { return particleFadeColour; }

    /** Null when the block above the core is not a machine block. */
    public static RuinMachineKind of(String coreBlockId) {
        if (coreBlockId == null) return null;
        for (RuinMachineKind kind : values()) if (kind.coreBlockId.equals(coreBlockId)) return kind;
        return null;
    }
}
