package dev.duyviet.gl2d3d;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mod configuration stored at {@code <configDir>/gl2d3d.json}.
 *
 * <p>Fields are public and mutable on purpose: Gson fills them reflectively,
 * and missing fields in an older file keep the defaults declared here.
 */
public final class Gl2D3dConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "gl2d3d.json";

    /** Master switch. When false the mod does nothing. */
    public boolean enabled = true;

    /** Folder (relative to the game directory) holding Mesa's opengl32.dll and friends. */
    public String mesaDir = "gl2d3d/mesa";

    /** Value of GALLIUM_DRIVER. "d3d12" routes OpenGL to DirectX 12. */
    public String driver = "d3d12";

    /** Extra environment variables passed to Mesa, e.g. MESA_GL_VERSION_OVERRIDE. */
    public Map<String, String> extraEnv = new LinkedHashMap<>();

    /**
     * Loads the config, writing a default file on first run.
     * Any I/O or parse error falls back to defaults so the game never fails to start.
     */
    public static Gl2D3dConfig load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Gl2D3dConfig loaded = GSON.fromJson(reader, Gl2D3dConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (Exception e) {
                Gl2D3dMod.LOGGER.warn("Could not read {}, using defaults", file, e);
            }
        }

        Gl2D3dConfig defaults = new Gl2D3dConfig();
        defaults.save(file);
        return defaults;
    }

    private void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            Gl2D3dMod.LOGGER.warn("Could not write default config {}", file, e);
        }
    }
}
