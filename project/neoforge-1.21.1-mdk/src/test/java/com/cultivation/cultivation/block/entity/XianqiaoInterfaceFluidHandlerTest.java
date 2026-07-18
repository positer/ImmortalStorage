package com.cultivation.cultivation.block.entity;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class XianqiaoInterfaceFluidHandlerTest {
    @Test
    void stableProxyFailsClosedThenUsesTheLiveFluidEndpointWithoutReplacement() {
        AtomicReference<IFluidHandler> endpoint = new AtomicReference<>();
        XianqiaoInterfaceFluidHandler proxy = new XianqiaoInterfaceFluidHandler(endpoint::get);
        FluidStack water = new FluidStack(Fluids.WATER, 1_000);

        assertEquals(0, proxy.getTanks());
        assertEquals(0, proxy.fill(water, IFluidHandler.FluidAction.EXECUTE));

        FluidTank liveStorage = new FluidTank(Integer.MAX_VALUE);
        endpoint.set(liveStorage);

        assertEquals(1_000, proxy.fill(water, IFluidHandler.FluidAction.SIMULATE));
        assertEquals(0, liveStorage.getFluidAmount(), "simulation must not mutate Xianqiao storage");
        assertEquals(1_000, proxy.fill(water, IFluidHandler.FluidAction.EXECUTE));
        assertEquals(1_000, liveStorage.getFluidAmount());

        endpoint.set(null);
        assertEquals(0, proxy.getTanks());
        assertEquals(0, proxy.drain(1_000, IFluidHandler.FluidAction.EXECUTE).getAmount());
        assertEquals(1_000, liveStorage.getFluidAmount(), "offline endpoints must fail closed");
    }
}
