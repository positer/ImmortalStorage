package com.immortalstorage.immortalstorage.locator;

import com.immortalstorage.immortalstorage.network.ToolLocatorSync;
import com.immortalstorage.immortalstorage.network.ToolLocatorFilterSync;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ToolLocatorSyncTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test void packetKeepsIndependentRangesOreVariantsAndRemovalDeltas() {
        var packet=new ToolLocatorSync(Identifier.parse("minecraft:overworld"),32,16,false,List.of(
                new ToolLocatorSync.Target(1,8,Identifier.parse("minecraft:iron_ore")),
                new ToolLocatorSync.Target(2,8,Identifier.parse("minecraft:deepslate_iron_ore")),
                new ToolLocatorSync.Target(3,1,null),new ToolLocatorSync.Target(4,0,null)));
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            ToolLocatorSync.STREAM_CODEC.encode(buf,packet);
            assertEquals(packet,ToolLocatorSync.STREAM_CODEC.decode(buf));
            assertEquals(0,buf.readableBytes());
        } finally { buf.release(); }
    }
    @Test void filterPolicyKeepsSelectionIdentityAndChunkCompletionFlags() {
        var packet=new ToolLocatorFilterSync(Identifier.parse("mod:drop"),true,true,true,false,List.of(Identifier.parse("mod:ore")));
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            ToolLocatorFilterSync.STREAM_CODEC.encode(buf,packet);
            assertEquals(packet,ToolLocatorFilterSync.STREAM_CODEC.decode(buf));assertEquals(0,buf.readableBytes());
        } finally {buf.release();}
    }
}
