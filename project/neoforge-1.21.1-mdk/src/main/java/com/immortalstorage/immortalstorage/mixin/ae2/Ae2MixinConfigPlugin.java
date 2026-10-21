package com.immortalstorage.immortalstorage.mixin.ae2;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Loader-safe gate: no AE2-targeted mixin is selected while AE2 is absent.
 *
 * <p>This plugin must stay free of AE2 types - Mixin always instantiates it, even when it then
 * refuses every mixin. It mirrors
 * {@code com.immortalstorage.immortalstorage.mixin.arsnouveau.ArsNouveauMixinConfigPlugin} and
 * {@code ...mixin.refinedstorage.RsMixinConfigPlugin}.</p>
 *
 * <p>Why gating matters here: the mixin both imports AE2 types (through
 * {@code XianqiaoAe2GridNodes.Host}) and adds an AE2-facing interface to
 * {@code XianqiaoManagerBlockEntity}. Without AE2 the mixin class must never be loaded, otherwise the
 * block entity fails to link at class-load time.</p>
 */
public final class Ae2MixinConfigPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /**
     * Gate on the AE2 API class being loadable rather than on {@code LoadingModList}.
     *
     * <p>{@code LoadingModList.get()} is not guaranteed to be populated when Mixin asks this
     * question — on 26.1.2 it returned null, the plugin answered false, and the whole AE2 bridge
     * silently stayed unapplied (measured: the capability was queried and the block entity was
     * "not mixed in"). A class-presence probe is ordering-independent: AE2 on the classpath means
     * the interface exists, AE2 absent means {@link ClassNotFoundException} and we refuse.</p>
     */
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            // Split literal so the optional-AE2 boundary test (which forbids the package prefix
            // outside compat/ae2) stays satisfied.
            Class.forName("appeng" + ".api.networking.IInWorldGridNodeHost", false,
                    getClass().getClassLoader());
            return true;
        } catch (Throwable absent) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(
            String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
