package dev.duyviet.gl2d3d;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

/**
 * After the client has started, reads the GL strings and reports whether the
 * game is really running on top of DirectX 12 (Mesa's d3d12 driver reports a
 * renderer string beginning with "D3D12").
 */
public final class RendererDiagnostics implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> logRenderer());
    }

    private static void logRenderer() {
        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        String version = GL11.glGetString(GL11.GL_VERSION);

        Gl2D3dMod.LOGGER.info("GL vendor={} | renderer={} | version={}", vendor, renderer, version);

        boolean viaD3D12 = renderer != null && renderer.toUpperCase(Locale.ROOT).contains("D3D12");
        if (viaD3D12) {
            Gl2D3dMod.LOGGER.info("Confirmed: OpenGL is running on DirectX 12.");
        } else {
            Gl2D3dMod.LOGGER.warn("Not running on D3D12 (renderer: {}). Check the Mesa folder and latest.log.", renderer);
        }
    }
}
