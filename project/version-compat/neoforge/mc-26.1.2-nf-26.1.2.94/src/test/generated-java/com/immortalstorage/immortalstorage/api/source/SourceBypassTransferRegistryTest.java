package com.immortalstorage.immortalstorage.api.source;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class SourceBypassTransferRegistryTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void brokenProvidersAreQuarantinedOnceAndLookupContinues() throws Exception {
        ServerLevel dummyLevel = (ServerLevel) unsafe().allocateInstance(ServerLevel.class);
        AtomicInteger runtimeCalls = new AtomicInteger();
        AtomicInteger linkageCalls = new AtomicInteger();
        AtomicInteger healthyCalls = new AtomicInteger();
        SourceBypassTransferTarget healthy = new SourceBypassTransferTarget() {};

        SourceBypassTransferRegistry.register(id("runtime_fault"), (level, pos, side) -> {
            if (level != dummyLevel) return null;
            runtimeCalls.incrementAndGet();
            throw new IllegalStateException("broken optional provider");
        });
        SourceBypassTransferRegistry.register(id("linkage_fault"), (level, pos, side) -> {
            if (level != dummyLevel) return null;
            linkageCalls.incrementAndGet();
            throw new NoSuchMethodError("optional API moved");
        });
        SourceBypassTransferRegistry.register(id("healthy"), (level, pos, side) -> {
            if (level != dummyLevel) return null;
            healthyCalls.incrementAndGet();
            return healthy;
        });

        assertSame(healthy, SourceBypassTransferRegistry.find(dummyLevel, BlockPos.ZERO, Direction.UP));
        assertSame(healthy, SourceBypassTransferRegistry.find(dummyLevel, BlockPos.ZERO, Direction.UP));
        assertEquals(1, runtimeCalls.get(), "a RuntimeException provider must be attempted only once");
        assertEquals(1, linkageCalls.get(), "a LinkageError provider must be attempted only once");
        assertEquals(2, healthyCalls.get(), "healthy providers remain available after another adapter is disabled");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("cultivation_test", path);
    }

    private static Unsafe unsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }
}
