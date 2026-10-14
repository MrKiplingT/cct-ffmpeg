package com.kiplingt.cctffmpeg.blockentity;

import com.kiplingt.cctffmpeg.CCTFFMPEG;
import com.kiplingt.cctffmpeg.peripheral.FFMPEGConverterPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class FFMPEGConverterBlockEntity extends BlockEntity implements Container {
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final FFMPEGConverterPeripheral peripheral = new FFMPEGConverterPeripheral(this);
    private volatile String status = "Idle";
    private volatile boolean busy = false;
    private volatile int jobId = 0;

    public FFMPEGConverterBlockEntity(BlockPos pos, BlockState state) { super(CCTFFMPEG.FFMPEG_CONVERTER_BE.get(), pos, state); }

    public FFMPEGConverterPeripheral getPeripheral() { return peripheral; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isBusy() { return busy; }
    public void setBusy(boolean busy) { this.busy = busy; }
    public int nextJobId() { return ++jobId; }

    @Nullable public java.nio.file.Path getDiskRoot(boolean create) {
        if (level == null || level.isClientSide || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return null;
        ItemStack disk = items.get(0);
        if (disk.isEmpty() || !isDisk(disk)) return null;
        String id = com.kiplingt.cctffmpeg.util.DiskStorage.ensureId(disk);
        setChanged();
        java.nio.file.Path root = serverLevel.getServer().getWorldPath(LevelResource.ROOT).resolve("cctffmpeg").resolve("disks").resolve(id).normalize();
        try { if (create) java.nio.file.Files.createDirectories(root); } catch (java.io.IOException e) { status = "Disk folder error: " + e.getMessage(); return null; }
        return root;
    }

    public static boolean isDisk(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.getNamespace().equals("computercraft") && key.getPath().startsWith("disk");
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return items.get(0).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? items.get(0) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return slot == 0 ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        items.set(0, stack);
        if (stack.getCount() > 1) stack.setCount(1);
        if (!stack.isEmpty() && isDisk(stack)) com.kiplingt.cctffmpeg.util.DiskStorage.ensureId(stack);
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && isDisk(stack); }
    @Override public void clearContent() { items.set(0, ItemStack.EMPTY); setChanged(); }
    @Override public boolean stillValid(Player player) { return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64.0; }
    @Override public void setChanged() { super.setChanged(); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
    }
}
