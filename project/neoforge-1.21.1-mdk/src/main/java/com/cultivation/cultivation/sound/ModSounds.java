package com.cultivation.cultivation.sound;

import com.cultivation.cultivation.CultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, CultivationMod.MODID);

    private ModSounds() {}
}
