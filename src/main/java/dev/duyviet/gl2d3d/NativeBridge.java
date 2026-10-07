package dev.duyviet.gl2d3d;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

import java.nio.file.Path;

/**
 * Thin JNA wrapper around the few native calls this mod needs.
 *
 * <p>Why the C runtime and not {@code SetEnvironmentVariableW}? Mesa reads its
 * settings with {@code getenv()}, which on Windows reads the CRT's own copy of the
 * environment. Only {@code _putenv_s} updates that copy (and the OS block too).
 */
final class NativeBridge {

    private interface Kernel32 extends Library {
        boolean SetDllDirectoryW(WString path);

        Pointer LoadLibraryW(WString path);
    }

    private interface Crt extends Library {
        int _putenv_s(String name, String value);
    }

    private NativeBridge() {
    }

    /**
     * Sets an environment variable in every C runtime Mesa might link against.
     *
     * @return true if at least one runtime accepted the value
     */
    static boolean setEnv(String name, String value) {
        boolean ok = false;
        // ucrtbase = modern MSVC builds; msvcrt = older / MinGW builds.
        for (String lib : new String[] {"ucrtbase", "msvcrt"}) {
            try {
                Crt crt = Native.load(lib, Crt.class);
                ok |= crt._putenv_s(name, value) == 0;
            } catch (UnsatisfiedLinkError e) {
                Gl2D3dMod.LOGGER.debug("C runtime {} not available", lib);
            }
        }
        return ok;
    }

    /**
     * Adds {@code dllDir} to the DLL search path, then loads {@code dll} by absolute path.
     * Once loaded, later {@code LoadLibrary("opengl32.dll")} calls (GLFW does this)
     * resolve to the already-loaded Mesa module because the base name matches.
     *
     * @return true if the DLL loaded
     */
    static boolean preloadDll(Path dllDir, Path dll) {
        Kernel32 kernel32 = Native.load("kernel32", Kernel32.class);
        kernel32.SetDllDirectoryW(new WString(dllDir.toAbsolutePath().toString()));
        Pointer handle = kernel32.LoadLibraryW(new WString(dll.toAbsolutePath().toString()));
        return handle != null;
    }
}
