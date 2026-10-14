package com.kiplingt.cctffmpeg.util;

import com.kiplingt.cctffmpeg.CCTFFMPEG;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FFMPEGRuntime {
    private FFMPEGRuntime() {}

    public static void run(Path input, Path output, Path diskRoot) throws Exception {
        Path executable = getExecutable();
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.add("-nostdin");
        command.add("-hide_banner");
        command.add("-y");
        command.add("-i");
        command.add(input.toString());
        command.add(output.toString());
        Path log = diskRoot.resolve(".ffmpeg-last-log.txt");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        int exit = process.waitFor();
        if (exit != 0) {
            String details = "";
            if (Files.exists(log)) {
                byte[] bytes = Files.readAllBytes(log);
                int start = Math.max(0, bytes.length - 3500);
                details = new String(bytes, start, bytes.length - start, java.nio.charset.StandardCharsets.UTF_8).replace('\n', ' ');
            }
            throw new IllegalStateException("FFmpeg exit code " + exit + (details.isBlank() ? "" : ": " + details));
        }
    }

    private static Path getExecutable() throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!(arch.equals("amd64") || arch.equals("x86_64"))) throw new IllegalStateException("Only x86-64 is supported currently");
        final String resource;
        final String filename;
        if (os.contains("win")) {
            resource = "/natives/windows-x86_64/ffmpeg.exe";
            filename = "ffmpeg.exe";
        } else if (os.contains("linux")) {
            resource = "/natives/linux-x86_64/ffmpeg";
            filename = "ffmpeg";
        } else {
            throw new IllegalStateException("Unsupported OS: " + os + " (Windows and Linux x86-64 are bundled)");
        }
        Path directory = Path.of(System.getProperty("user.dir"), "config", "cctffmpeg", "bin");
        Files.createDirectories(directory);
        Path executable = directory.resolve(filename);
        if (!Files.isRegularFile(executable) || Files.size(executable) < 1024) {
            try (InputStream in = FFMPEGRuntime.class.getResourceAsStream(resource)) {
                if (in == null) throw new IllegalStateException("Bundled FFmpeg executable missing: " + resource);
                Path temporary = directory.resolve(filename + ".tmp");
                Files.copy(in, temporary, StandardCopyOption.REPLACE_EXISTING);
                try { Files.move(temporary, executable, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, executable, StandardCopyOption.REPLACE_EXISTING); }
            }
        }
        if (!os.contains("win")) {
            try { executable.toFile().setExecutable(true, true); }
            catch (Exception ignored) {}
            try { Files.setPosixFilePermissions(executable, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x")); }
            catch (UnsupportedOperationException | java.io.IOException ignored) {}
        }
        return executable;
    }
}
