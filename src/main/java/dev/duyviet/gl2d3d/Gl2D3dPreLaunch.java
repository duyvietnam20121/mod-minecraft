package dev.duyviet.gl2d3d;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Runs before Minecraft touches OpenGL and redirects the GL library to Mesa's
 * Gallium "d3d12" driver, which translates OpenGL calls to DirectX 12.
 *
 * <p>Order matters: environment first (Mesa reads it when the DLL initialises),
 * then the DLL is preloaded, then LWJGL is told which library to use.
 * Every failure path falls back to the system OpenGL driver instead of crashing.
 */
public final class Gl2D3dPreLaunch implements PreLaunchEntrypoint {

    private static final String LWJGL_GL_LIB_PROPERTY = "org.lwjgl.opengl.libname";
    private static final List<String> REQUIRED_FILES = List.of("opengl32.dll", "libgallium_wgl.dll");

    @Override
    public void onPreLaunch() {
        if (!isWindows()) {
            Gl2D3dMod.LOGGER.warn("DirectX is only available on Windows; leaving OpenGL untouched.");
            return;
        }

        FabricLoader loader = FabricLoader.getInstance();
        Gl2D3dConfig config = Gl2D3dConfig.load(loader.getConfigDir());
        if (!config.enabled) {
            Gl2D3dMod.LOGGER.info("Disabled in config; using the default OpenGL driver.");
            return;
        }

        Path mesaDir = loader.getGameDir().resolve(config.mesaDir).toAbsolutePath().normalize();
        List<String> missing = findMissingFiles(mesaDir);
        if (!missing.isEmpty()) {
            Gl2D3dMod.LOGGER.warn("Mesa files missing in {}: {}. Falling back to default OpenGL.", mesaDir, missing);
            return;
        }

        applyEnvironment(config);

        Path mesaGl = mesaDir.resolve("opengl32.dll");
        try {
            if (!NativeBridge.preloadDll(mesaDir, mesaGl)) {
                Gl2D3dMod.LOGGER.error("Could not load {}. Falling back to default OpenGL.", mesaGl);
                return;
            }
        } catch (UnsatisfiedLinkError | RuntimeException e) {
            Gl2D3dMod.LOGGER.error("Native bridge failed. Falling back to default OpenGL.", e);
            return;
        }

        System.setProperty(LWJGL_GL_LIB_PROPERTY, mesaGl.toString());
        Gl2D3dMod.LOGGER.info("OpenGL routed through Mesa ({}) from {}", config.driver, mesaDir);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static List<String> findMissingFiles(Path dir) {
        return REQUIRED_FILES.stream()
                .filter(name -> !Files.isRegularFile(dir.resolve(name)))
                .toList();
    }

    /** Builds the final env map (driver first, user overrides last) and applies it. */
    private static void applyEnvironment(Gl2D3dConfig config) {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("GALLIUM_DRIVER", config.driver);
        if (config.extraEnv != null) {
            env.putAll(config.extraEnv);
        }

        env.forEach((name, value) -> {
            if (!NativeBridge.setEnv(name, value)) {
                Gl2D3dMod.LOGGER.warn("Could not set environment variable {}", name);
            }
        });
    }
}
