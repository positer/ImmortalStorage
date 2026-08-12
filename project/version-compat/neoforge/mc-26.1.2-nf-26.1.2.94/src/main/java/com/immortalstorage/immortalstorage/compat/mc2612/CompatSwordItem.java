package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.world.item.Item;

/** 26.1 has no legacy SwordItem class; weapon properties carry the material. */
public class CompatSwordItem extends CompatItem {
    public CompatSwordItem(Item.Properties properties) {
        super(properties);
    }
}
