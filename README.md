# GL to D3D (Minecraft 1.21.4, Fabric)

Routes Minecraft's OpenGL calls to **DirectX 12** by loading **Mesa's Gallium `d3d12` driver**
instead of the system `opengl32.dll`. The mod itself contains no renderer; it only wires Mesa in.

Target: Minecraft 1.21.4, Fabric Loader 0.19.3, Fabric API 0.119.4, Java 21, Windows only.

## Build

```
gradle wrapper --gradle-version 8.12
./gradlew build
```

The jar is written to `build/libs/gl2d3d-1.0.0.jar`.

## Install

1. Put the jar in `mods/`.
2. Download a Windows Mesa build that includes the `d3d12` Gallium driver
   (for example the releases of `pal1000/mesa-dist-win` on GitHub; pick the
   build for your CPU architecture and verify the package contains d3d12 support).
3. Create `<game dir>/gl2d3d/mesa/` and copy in at least:
   - `opengl32.dll`
   - `libgallium_wgl.dll`
   - any other DLLs shipped next to them (for example `libglapi.dll`)
   - `dxil.dll` (needed by the d3d12 driver to compile shaders; it comes from
     the Windows SDK / DirectX Shader Compiler if the Mesa package lacks it)
4. Launch the game. `config/gl2d3d.json` is created on first run.

## Verify

Look in `logs/latest.log` for:

```
OpenGL routed through Mesa (d3d12) from ...
Confirmed: OpenGL is running on DirectX 12.
```

The F3 screen should also show a renderer string starting with `D3D12`.

## Config (`config/gl2d3d.json`)

| Field | Meaning |
|---|---|
| `enabled` | Master switch |
| `mesaDir` | Mesa folder, relative to the game directory |
| `driver` | Value of `GALLIUM_DRIVER` (`d3d12`) |
| `extraEnv` | Extra Mesa env vars, e.g. `{"MESA_GL_VERSION_OVERRIDE": "4.6"}` |

## Known limits

- Performance can be lower or higher than a native OpenGL driver depending on your GPU.
- Some mods that replace the renderer (Sodium, Iris) may behave differently or fail.
- If any file is missing or loading fails, the mod logs a warning and Minecraft starts with normal OpenGL.
