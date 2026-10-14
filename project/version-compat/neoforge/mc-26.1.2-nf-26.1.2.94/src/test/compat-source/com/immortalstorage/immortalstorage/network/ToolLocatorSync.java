package com.immortalstorage.immortalstorage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

/** Per-player cache deltas; a zero mask removes a target and reset clears the previous window. */
public record ToolLocatorSync(Identifier dimension, int radius, int miningRadius, boolean reset, List<Target> targets)
        implements CustomPacketPayload {
    public record Target(long position, int mask, Identifier oreBlock) {
        public Target {
            if (mask < 0 || mask > 15 || ((mask & 8) != 0) != (oreBlock != null)) throw new IllegalArgumentException("locator target");
        }
    }
    public static final Type<ToolLocatorSync> TYPE = new Type<>(Identifier.fromNamespaceAndPath("immortalstorage", "tool_locator"));
    public ToolLocatorSync {
        if (radius < 1 || radius > 48 || miningRadius < 1 || miningRadius > 48 || targets.size() > 4096) throw new IllegalArgumentException("locator packet bounds");
        targets = List.copyOf(targets);
    }
    @Override public Type<ToolLocatorSync> type() { return TYPE; }
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolLocatorSync> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeIdentifier(packet.dimension); buf.writeVarInt(packet.radius); buf.writeBoolean(packet.reset);
                buf.writeVarInt(packet.miningRadius);
                buf.writeVarInt(packet.targets.size());
                for (var target : packet.targets) {
                    buf.writeLong(target.position); buf.writeByte(target.mask);
                    if (target.oreBlock != null) buf.writeIdentifier(target.oreBlock);
                }
            }, buf -> {
                var dimension = buf.readIdentifier(); int radius = buf.readVarInt(); boolean reset = buf.readBoolean();
                int miningRadius = buf.readVarInt();
                int size = buf.readVarInt(); if (size < 0 || size > 4096) throw new IllegalArgumentException("locator target count");
                var targets = new ArrayList<Target>(size);
                for (int i = 0; i < size; i++) {
                    long position = buf.readLong(); int mask = buf.readUnsignedByte();
                    targets.add(new Target(position, mask, (mask & 8) != 0 ? buf.readIdentifier() : null));
                }
                return new ToolLocatorSync(dimension, radius, miningRadius, reset, targets);
            });
}
