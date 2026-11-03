package kurospotify;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.*;

public class Player {
    public static volatile boolean active, playing;
    public static volatile String id = "", title = "", artist = "";
    public static volatile long durMs, progMs, stamp;

    static final HttpClient H = HttpClient.newHttpClient();

    public static void start() {
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "kurospotify-poll"); t.setDaemon(true); return t; });
        ex.scheduleWithFixedDelay(Player::poll, 0, 1200, TimeUnit.MILLISECONDS);
    }

    /** Su anki konum (ms), yerel olarak ilerletilir. */
    public static double pos() {
        double p = progMs;
        if (playing) p += (System.nanoTime() - stamp) / 1e6;
        return Math.min(p, durMs);
    }

    static HttpRequest.Builder req(String path, String tok) {
        return HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1" + path)).header("Authorization", "Bearer " + tok);
    }

    static void poll() {
        try {
            String tok = Auth.get();
            if (tok == null) { active = false; return; }
            HttpResponse<String> rs = H.send(req("/me/player", tok).GET().build(), BodyHandlers.ofString());
            int c = rs.statusCode();
            if (c == 204) { active = false; playing = false; return; }
            if (c == 401) { Auth.expires = 0; return; }
            if (c != 200) return;
            JsonObject o = JsonParser.parseString(rs.body()).getAsJsonObject();
            JsonElement it = o.get("item");
            if (it == null || it.isJsonNull()) { active = false; return; }
            JsonObject t = it.getAsJsonObject();
            StringBuilder a = new StringBuilder();
            for (JsonElement e : t.getAsJsonArray("artists")) {
                if (a.length() > 0) a.append(", ");
                a.append(e.getAsJsonObject().get("name").getAsString());
            }
            title = t.get("name").getAsString();
            artist = a.toString();
            durMs = t.get("duration_ms").getAsLong();
            progMs = o.has("progress_ms") && !o.get("progress_ms").isJsonNull() ? o.get("progress_ms").getAsLong() : 0;
            stamp = System.nanoTime();
            playing = o.has("is_playing") && o.get("is_playing").getAsBoolean();
            id = t.get("id").isJsonNull() ? title : t.get("id").getAsString();
            active = true;
        } catch (Exception ignored) {}
    }

    static void cmd(String method, String path) {
        CompletableFuture.runAsync(() -> {
            try {
                String tok = Auth.get();
                if (tok == null) return;
                H.send(req(path, tok).method(method, BodyPublishers.noBody()).build(), BodyHandlers.discarding());
                poll();
            } catch (Exception ignored) {}
        });
    }

    public static void toggle() { if (playing) { playing = false; cmd("PUT", "/me/player/pause"); } else { cmd("PUT", "/me/player/play"); } }
    public static void next() { cmd("POST", "/me/player/next"); }
    public static void prev() { cmd("POST", "/me/player/previous"); }
}
