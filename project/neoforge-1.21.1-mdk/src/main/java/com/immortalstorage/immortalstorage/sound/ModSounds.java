package com.immortalstorage.immortalstorage.sound;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, ImmortalStorageMod.MODID);

    private ModSounds() {}
}
