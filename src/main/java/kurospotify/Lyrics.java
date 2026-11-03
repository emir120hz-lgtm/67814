package kurospotify;

import com.google.gson.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** lrclib.net uzerinden senkronize sozler. */
public class Lyrics {
    public static volatile long[] times = new long[0];
    public static volatile String[] lines = new String[0];
    public static volatile boolean loading;
    static volatile String forTrack = "";
    static final Pattern LRC = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)");
    static final HttpClient H = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(8)).build();

    public static void tick() {
        String id = Player.id;
        if (!Player.active || id.isEmpty() || id.equals(forTrack)) return;
        forTrack = id; loading = true;
        times = new long[0]; lines = new String[0];
        final String t = Player.title, a = Player.artist; final long d = Player.durMs;
        Thread th = new Thread(() -> {
            try { load(id, a, t, d); } catch (Exception ignored) {}
            loading = false;
        }, "kurospotify-lyrics");
        th.setDaemon(true); th.start();
    }

    static String get(String url) throws Exception {
        HttpResponse<String> r = H.send(HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "kurospotify/1.0").GET().build(), BodyHandlers.ofString());
        return r.statusCode() == 200 ? r.body() : null;
    }
    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    static void load(String id, String artist, String title, long dur) throws Exception {
        String clean = title.replaceAll("\\s*[-(\\[]\\s*(?i:remaster|live|mono|stereo|version|edit|radio|single|from).*$", "").trim();
        String firstArtist = artist.split(",")[0].trim();
        String synced = null;
        String body = get("https://lrclib.net/api/get?artist_name=" + enc(firstArtist) + "&track_name=" + enc(clean) + "&duration=" + Math.round(dur / 1000.0));
        if (body != null) synced = str(JsonParser.parseString(body).getAsJsonObject());
        if (synced == null) {
            body = get("https://lrclib.net/api/search?track_name=" + enc(clean) + "&artist_name=" + enc(firstArtist));
            if (body != null) for (JsonElement e : JsonParser.parseString(body).getAsJsonArray()) {
                synced = str(e.getAsJsonObject()); if (synced != null) break;
            }
        }
        if (synced == null || !id.equals(forTrack)) return;
        List<Long> ts = new ArrayList<>(); List<String> ls = new ArrayList<>();
        for (String ln : synced.split("\\r?\\n")) {
            Matcher m = LRC.matcher(ln.trim());
            if (!m.matches()) continue;
            ts.add((long) (Integer.parseInt(m.group(1)) * 60000L + Double.parseDouble(m.group(2)) * 1000));
            ls.add(m.group(3).trim());
        }
        long[] ta = new long[ts.size()];
        for (int i = 0; i < ta.length; i++) ta[i] = ts.get(i);
        lines = ls.toArray(new String[0]); times = ta;
    }

    static String str(JsonObject o) {
        JsonElement e = o.get("syncedLyrics");
        return e == null || e.isJsonNull() || e.getAsString().isBlank() ? null : e.getAsString();
    }

    /** posMs anindaki satir indeksi, yoksa -1. */
    public static int indexAt(double posMs) {
        long[] t = times; int r = -1;
        for (int i = 0; i < t.length; i++) { if (t[i] <= posMs) r = i; else break; }
        return r;
    }
}
