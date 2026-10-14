package com.kiplingt.cctffmpeg.peripheral;

import com.kiplingt.cctffmpeg.blockentity.FFMPEGConverterBlockEntity;
import com.kiplingt.cctffmpeg.util.FFMPEGRuntime;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public final class FFMPEGConverterPeripheral implements IPeripheral {
    private static final long DISK_CAPACITY = 1_073_741_824L;
    private final FFMPEGConverterBlockEntity blockEntity;
    private final Set<IComputerAccess> attached = ConcurrentHashMap.newKeySet();
    private final java.util.Map<IComputerAccess, String> mountedPaths = new ConcurrentHashMap<>();

    public FFMPEGConverterPeripheral(FFMPEGConverterBlockEntity blockEntity) { this.blockEntity = blockEntity; }

    @Override public String getType() { return "ffmpeg_converter"; }
    @Override public boolean equals(@Nullable IPeripheral other) { return other instanceof FFMPEGConverterPeripheral p && p.blockEntity == blockEntity; }

    @Override public void attach(IComputerAccess computer) {
        attached.add(computer);
        mountDisk(computer);
    }

    @Override public void detach(IComputerAccess computer) {
        attached.remove(computer);
        String mountedPath = mountedPaths.remove(computer);
        if (mountedPath != null) computer.unmount(mountedPath);
    }

    private void mountDisk(IComputerAccess computer) {
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) return;
        Path root = blockEntity.getDiskRoot(true);
        if (root == null) return;
        try {
            WritableMount mount = ComputerCraftAPI.createSaveDirMount(level.getServer(), "cctffmpeg/disks/" + root.getFileName(), DISK_CAPACITY);
            String mountedPath = computer.mountWritable("ffmpeg_disk", mount);
            if (mountedPath != null) mountedPaths.put(computer, mountedPath);
        } catch (Exception e) {
            blockEntity.setStatus("Could not mount disk: " + e.getMessage());
        }
    }

    @LuaFunction(mainThread = true)
    public final boolean refreshDisk() {
        for (IComputerAccess computer : attached) {
            String mountedPath = mountedPaths.remove(computer);
            if (mountedPath != null) computer.unmount(mountedPath);
            mountDisk(computer);
        }
        return blockEntity.getDiskRoot(false) != null;
    }

    @LuaFunction(mainThread = true)
    public final String getStatus() {
        return blockEntity.getStatus();
    }

    @LuaFunction(mainThread = true)
    public final boolean isBusy() { return blockEntity.isBusy(); }

    @LuaFunction(mainThread = true)
    public final boolean hasDisk() { return blockEntity.getDiskRoot(false) != null; }

    @LuaFunction(mainThread = true)
    public final String convert(String input, String output) throws LuaException {
        if (blockEntity.isBusy()) throw new LuaException("A conversion is already running");
        Path root = blockEntity.getDiskRoot(true);
        if (root == null) throw new LuaException("Insert a CC:Tweaked disk first");
        final Path inputPath = resolveSafe(root, input);
        final Path outputPath = resolveSafe(root, output);
        if (inputPath.equals(outputPath)) throw new LuaException("Input and output must be different files");
        if (!Files.isRegularFile(inputPath)) throw new LuaException("Input file does not exist on ffmpeg_disk");
        if (Files.exists(outputPath)) throw new LuaException("Output file already exists");
        try { Files.createDirectories(outputPath.getParent()); }
        catch (Exception e) { throw new LuaException("Could not create output folder: " + e.getMessage()); }

        int id = blockEntity.nextJobId();
        blockEntity.setBusy(true);
        blockEntity.setStatus("Queued conversion #" + id);
        Thread worker = new Thread(() -> {
            try {
                blockEntity.setStatus("Converting " + input + " -> " + output);
                FFMPEGRuntime.run(inputPath, outputPath, root);
                blockEntity.setStatus("Conversion #" + id + " complete");
            } catch (Exception e) {
                blockEntity.setStatus("Conversion failed: " + safeMessage(e));
            } finally {
                blockEntity.setBusy(false);
            }
        }, "cctffmpeg-convert-" + id);
        worker.setDaemon(true);
        worker.start();
        return "started:" + id;
    }

    private static Path resolveSafe(Path root, String name) throws LuaException {
        if (name == null || name.isBlank() || name.indexOf('\0') >= 0) throw new LuaException("Invalid file path");
        Path relative;
        try { relative = Path.of(name.replace('\\', '/')); }
        catch (Exception e) { throw new LuaException("Invalid file path"); }
        if (relative.isAbsolute()) throw new LuaException("Use a path relative to ffmpeg_disk");
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root.normalize()) || relative.toString().contains("..")) throw new LuaException("Path traversal is not allowed");
        return resolved;
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null ? e.getClass().getSimpleName() : message;
    }
}
