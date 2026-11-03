package kurospotify;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SpotifyMod implements ClientModInitializer {
    static KeyBinding open;

    @Override public void onInitializeClient() {
        Cfg.load();
        Player.start();
        open = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.kurospotify.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.kurospotify"));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (open.wasPressed()) mc.setScreen(new SpotifyScreen());
        });
        HudRenderCallback.EVENT.register((ctx, tick) -> Hud.render(ctx));
    }
}
