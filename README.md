# CC:T FFMPEG Converter

NeoForge mod target: Minecraft 1.21.1, NeoForge 21.1.x, CC:Tweaked 1.120.x.

## Current implementation

- Registers one block: **FFMPEG Converter** (`cctffmpeg:ffmpeg_converter`).
- Crafting recipe uses smooth stone, a CC:Tweaked advanced monitor, comparators, a CC:Tweaked disk drive, and a CC:Tweaked speaker.
- Right-click opens a disk-drive-style inventory GUI with one slot. It accepts CC:Tweaked disk items.
- Exposes the `ffmpeg_converter` peripheral.
- When a computer attaches while a disk is inserted, the disk's persistent output folder is mounted on the computer as `ffmpeg_disk`.
- `peripheral.getStatus()`, `peripheral.isBusy()`, `peripheral.hasDisk()`, and `peripheral.convert(input, output)`, and `peripheral.refreshDisk()` are available.
- `convert` reads and writes paths relative to `ffmpeg_disk`, runs asynchronously, and chooses output format from the output extension.
- FFmpeg binaries are bundled for Windows x86-64 and Linux x86-64 and extracted to `config/cctffmpeg/bin` at runtime.

Example ComputerCraft Lua:

```lua
local ffmpeg = peripheral.find("ffmpeg_converter")
if not ffmpeg then error("FFMPEG Converter not found") end

-- The disk inserted into the converter is mounted at /ffmpeg_disk.
-- Copy a media file from the computer onto the inserted disk first.
fs.copy("/input.mp4", "/ffmpeg_disk/input.mp4")
local job = ffmpeg.convert("input.mp4", "output.webm")
print(job)

while ffmpeg.isBusy() do
  print(ffmpeg.getStatus())
  sleep(1)
end
print(ffmpeg.getStatus())
```

The inserted CC:Tweaked disk stores data in the world save under `cctffmpeg/disks/<disk-id>`. It is mounted into the computer as `ffmpeg_disk` when the computer connects. If you insert or replace a disk after the computer is already connected, call `peripheral.refreshDisk()` to refresh the mount.

## Build

Use Java 21 and run `./gradlew build` (Windows: `gradlew.bat build`). The build needs network access to fetch Gradle, NeoForge, and the CC:Tweaked API dependencies.

## Bundled FFmpeg

The Linux binary's accompanying LGPL v3 license is in `src/main/resources/licenses/FFmpeg-LGPLv3.txt`. FFmpeg is a separate upstream executable; retain its license and corresponding upstream notices when redistributing this mod. The Windows executable was supplied separately and should be verified against the intended FFmpeg build/license before public redistribution.

## Known first-pass limitations

- Only Windows and Linux x86-64 are supported. macOS and ARM builds are not included.
- Conversion uses FFmpeg defaults and infers the output format from the output filename. Advanced codec/argument controls, cancellation, progress percentages, and GUI status synchronization are not implemented yet.
- The project has not been compiled in this environment because the Gradle distribution cannot be downloaded here; run a local build and report any compiler errors for the next correction pass.
