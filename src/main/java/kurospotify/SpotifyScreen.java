package kurospotify;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SpotifyScreen extends Screen {
    TextFieldWidget id;

    public SpotifyScreen() { super(Text.literal("Spotify")); }

    @Override protected void init() {
        int cx = width / 2, y = height / 2 - 70;
        id = new TextFieldWidget(textRenderer, cx - 120, y, 240, 20, Text.literal("Client ID"));
        id.setMaxLength(64); id.setText(Cfg.I.clientId); id.setPlaceholder(Text.literal("Spotify Client ID"));
        id.setChangedListener(s -> { Cfg.I.clientId = s.trim(); Cfg.save(); });
        addDrawableChild(id);
        addDrawableChild(ButtonWidget.builder(Text.literal("Giris yap"), b -> Auth.login()).dimensions(cx - 120, y + 26, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cikis"), b -> Auth.logout()).dimensions(cx + 2, y + 26, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("<<"), b -> Player.prev()).dimensions(cx - 120, y + 60, 76, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Oynat / Durdur"), b -> Player.toggle()).dimensions(cx - 40, y + 60, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">>"), b -> Player.next()).dimensions(cx + 44, y + 60, 76, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cubuk: " + (Cfg.I.bar ? "ACIK" : "KAPALI")), b -> {
            Cfg.I.bar = !Cfg.I.bar; Cfg.save(); b.setMessage(Text.literal("Cubuk: " + (Cfg.I.bar ? "ACIK" : "KAPALI"))); })
            .dimensions(cx - 120, y + 86, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sozler: " + (Cfg.I.words ? "ACIK" : "KAPALI")), b -> {
            Cfg.I.words = !Cfg.I.words; Cfg.save(); b.setMessage(Text.literal("Sozler: " + (Cfg.I.words ? "ACIK" : "KAPALI"))); })
            .dimensions(cx + 2, y + 86, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Soz -0.25s"), b -> { Cfg.I.offsetMs -= 250; Cfg.save(); })
            .dimensions(cx - 120, y + 112, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Soz +0.25s"), b -> { Cfg.I.offsetMs += 250; Cfg.save(); })
            .dimensions(cx + 2, y + 112, 118, 20).build());
    }

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        int cx = width / 2, y = height / 2 - 70;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Spotify"), cx, y - 18, 0xFF1DB954);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(Auth.status + (Auth.loggedIn() ? "  (bagli)" : "")), cx, y + 140, 0xFFFFFFFF);
        if (Player.active) ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(Player.title + " - " + Player.artist + "  [offset " + Cfg.I.offsetMs + " ms]"), cx, y + 154, 0xFFB388FF);
    }
}
