package com.kiplingt.cctffmpeg.util;

import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class DiskStorage {
    private static final String ID_KEY = "cctffmpeg_disk_id";
    private DiskStorage() {}

    public static String ensureId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData == null ? new CompoundTag() : customData.copyTag();
        if (!tag.hasUUID(ID_KEY)) {
            tag.putUUID(ID_KEY, UUID.randomUUID());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return tag.getUUID(ID_KEY).toString();
    }
}
