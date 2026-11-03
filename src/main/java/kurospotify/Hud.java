package kurospotify;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import java.util.*;

public class Hud {
    static class Word { String text; float x, y, rot, scale; long spawn, die = Long.MAX_VALUE; }
    static final List<Word> words = new ArrayList<>();
    static int lastIdx = -2; static String lastTrack = "";
    static final Locale TR = Locale.forLanguageTag("tr");

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        Lyrics.tick();
        if (!Player.active) return;
        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        double pos = Player.pos() + Cfg.I.offsetMs;
        int idx = Lyrics.indexAt(pos);
        if (!Player.id.equals(lastTrack)) { lastTrack = Player.id; lastIdx = -2; markDead(); }
        if (idx != lastIdx) { lastIdx = idx; newLine(idx); }

        if (Cfg.I.bar) drawBar(ctx, tr, sw, idx, pos);
        if (Cfg.I.words) drawWords(ctx, tr, sw / 2f, sh / 2f);
    }

    static void drawBar(DrawContext ctx, TextRenderer tr, int sw, int idx, double pos) {
        int w = 250, h = 14, x = (sw - w) / 2, y = 4;
        ctx.fill(x, y, x + w, y + h, 0xB0000000);
        ctx.fill(x + 3, y + 4, x + 9, y + 10, Player.playing ? 0xFF1DB954 : 0xFF777777);
        float prog = Player.durMs > 0 ? (float) (Player.pos() / Player.durMs) : 0;
        ctx.fill(x, y + h - 1, x + (int) (w * prog), y + h, 0xFF1DB954);
        String line = idx >= 0 && idx < Lyrics.lines.length ? Lyrics.lines[idx] : "";
        if (line.isEmpty()) line = Player.title + " - " + Player.artist;
        line = line.toUpperCase(TR);
        int tx = x + 14;
        ctx.enableScissor(tx, y, x + w - 4, y + h);
        // aktif satirin ilerlemesine gore mor vurgu
        double start = idx >= 0 ? Lyrics.times[idx] : 0;
        double end = idx >= 0 && idx + 1 < Lyrics.times.length ? Lyrics.times[idx + 1] : start + 4000;
        float f = (float) Math.max(0, Math.min(1, (pos - start) / Math.max(1, (end - start) * 0.85)));
        int n = idx >= 0 ? (int) (line.length() * f) : 0;
        String a = line.substring(0, n), b = line.substring(n);
        int total = tr.getWidth(line), avail = x + w - 4 - tx;
        int shift = total > avail ? -(int) ((total - avail) * f) : 0;
        ctx.drawText(tr, a, tx + shift, y + 3, 0xFFB388FF, false);
        ctx.drawText(tr, b, tx + shift + tr.getWidth(a), y + 3, 0xFFFFFFFF, false);
        ctx.disableScissor();
    }

    static void markDead() {
        long now = System.currentTimeMillis();
        for (Word w : words) if (w.die == Long.MAX_VALUE) w.die = now + 300;
    }

    static void newLine(int idx) {
        markDead();
        if (idx < 0 || idx >= Lyrics.lines.length || Lyrics.lines[idx].isBlank()) return;
        String[] parts = Lyrics.lines[idx].toUpperCase(TR).split("\\s+");
        long dur = idx + 1 < Lyrics.times.length ? Lyrics.times[idx + 1] - Lyrics.times[idx] : 3000;
        dur = Math.max(800, Math.min(dur, 6000));
        Random r = new Random(Lyrics.times[idx]);
        long now = System.currentTimeMillis();
        int n = parts.length;
        for (int i = 0; i < n; i++) {
            Word w = new Word();
            w.text = parts[i];
            double ang = r.nextDouble() * Math.PI * 2;
            double rad = 45 + r.nextDouble() * 75;
            w.x = (float) (Math.cos(ang) * rad * 1.5);
            w.y = (float) (Math.sin(ang) * rad * 0.7);
            w.rot = (r.nextFloat() - 0.5f) * 30f;
            w.scale = 1.1f + r.nextFloat() * 0.9f;
            w.spawn = now + (long) (dur * 0.8 * i / Math.max(1, n));
            words.add(w);
        }
    }

    static void drawWords(DrawContext ctx, TextRenderer tr, float cx, float cy) {
        long now = System.currentTimeMillis();
        words.removeIf(w -> now > w.die);
        MatrixStack m = ctx.getMatrices();
        for (Word w : words) {
            if (now < w.spawn) continue;
            float age = (now - w.spawn) / 1000f;
            float pop = Math.min(1f, age / 0.18f);
            float c1 = 1.70158f, c3 = c1 + 1f, p = pop - 1f;
            float ease = 1f + c3 * p * p * p + c1 * p * p; // easeOutBack
            float s = w.scale * (0.3f + 0.7f * ease);
            float alpha = Math.min(1f, age / 0.12f);
            if (w.die != Long.MAX_VALUE) alpha *= Math.max(0f, (w.die - now) / 300f);
            int a = (int) (alpha * 255);
            if (a < 6) continue;
            float drift = age * 4f;
            m.push();
            m.translate(cx + w.x, cy + w.y - drift, 0);
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(w.rot));
            m.scale(s, s, 1f);
            ctx.drawText(tr, w.text, -tr.getWidth(w.text) / 2, -4, (a << 24) | 0xFFFFFF, true);
            m.pop();
        }
    }
}
