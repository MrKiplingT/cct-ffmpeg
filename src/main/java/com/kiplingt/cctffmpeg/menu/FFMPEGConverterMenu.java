package com.kiplingt.cctffmpeg.menu;

import com.kiplingt.cctffmpeg.CCTFFMPEG;
import com.kiplingt.cctffmpeg.blockentity.FFMPEGConverterBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FFMPEGConverterMenu extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerLevelAccess access;

    public FFMPEGConverterMenu(int containerId, Inventory playerInventory, @Nullable FFMPEGConverterBlockEntity blockEntity) {
        super(CCTFFMPEG.FFMPEG_CONVERTER_MENU.get(), containerId);
        this.inventory = blockEntity;
        this.access = blockEntity == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (blockEntity != null) {
            checkContainerSize(blockEntity, 1);
            addSlot(new Slot(blockEntity, 0, 80, 35) {
                @Override public boolean mayPlace(ItemStack stack) { return blockEntity.canPlaceItem(0, stack); }
            });
        } else {
            addSlot(new Slot(new EmptyContainer(), 0, 80, 35) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return empty;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index == 0) {
            if (!moveItemStackTo(original, 1, slots.size(), true)) return empty;
        } else if (inventory != null && inventory.canPlaceItem(0, original)) {
            if (!moveItemStackTo(original, 0, 1, false)) return empty;
        } else if (index < 28) {
            if (!moveItemStackTo(original, 28, 37, false)) return empty;
        } else if (!moveItemStackTo(original, 1, 28, false)) return empty;
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == copy.getCount()) return empty;
        slot.onTake(player, original);
        return copy;
    }

    @Override public boolean stillValid(Player player) {
        return inventory != null && inventory.stillValid(player) && stillValid(access, player, CCTFFMPEG.FFMPEG_CONVERTER.get());
    }

    private static final class EmptyContainer implements Container {
        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return true; }
        @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
        @Override public void setItem(int slot, ItemStack stack) {}
        @Override public void setChanged() {}
        @Override public boolean stillValid(Player player) { return false; }
        @Override public void clearContent() {}
    }
}
