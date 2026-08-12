package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.compat.CompatManager;
import net.minecraft.world.item.Item;

import java.lang.reflect.InvocationTargetException;

/** Creates the RS implementation only after the optional mod is known present. */
public final class RsExchangeDiskFactory {
    private static final String IMPLEMENTATION =
            "com.immortalstorage.immortalstorage.compat.refinedstorage.XianqiaoRsStorageContainerItem";

    public static Item create(Item.Properties properties) {
        if (!CompatManager.RS_LOADED) return new XianqiaoRsExchangeDiskItem(properties);
        try {
            Class<?> type = Class.forName(IMPLEMENTATION, true, RsExchangeDiskFactory.class.getClassLoader());
            return (Item) type.getConstructor(Item.Properties.class).newInstance(properties);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException("Refined Storage exchange disk failed to initialize", cause);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Refined Storage 2.0.9 API is incompatible", exception);
        }
    }

    private RsExchangeDiskFactory() {
    }
}
