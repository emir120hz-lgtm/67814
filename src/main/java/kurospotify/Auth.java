package kurospotify;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.util.Util;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/** Spotify OAuth (PKCE). Client secret gerekmez, sadece Client ID. */
public class Auth {
    public static final String REDIRECT = "http://127.0.0.1:8888/callback";
    static final HttpClient H = HttpClient.newHttpClient();
    static volatile String access = "";
    static volatile long expires = 0;
    public static volatile String status = "Giris yapilmadi";
    static HttpServer srv;

    public static boolean loggedIn() { return !Cfg.I.refreshToken.isEmpty(); }

    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    public static synchronized void login() {
        if (Cfg.I.clientId.isBlank()) { status = "Once Client ID gir"; return; }
        try {
            if (srv != null) srv.stop(0);
            byte[] r = new byte[64]; new SecureRandom().nextBytes(r);
            final String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(r);
            String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
            final HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 8888), 0);
            srv = s;
            s.createContext("/callback", ex -> {
                String q = ex.getRequestURI().getRawQuery(), code = null;
                if (q != null) for (String p : q.split("&")) if (p.startsWith("code="))
                    code = URLDecoder.decode(p.substring(5), StandardCharsets.UTF_8);
                byte[] b = ("<html><body style='font-family:sans-serif;background:#111;color:#1db954;text-align:center;padding-top:20vh'>"
                    + "<h2>Giris tamam, oyuna donebilirsin.</h2></body></html>").getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                ex.sendResponseHeaders(200, b.length);
                try (var o = ex.getResponseBody()) { o.write(b); }
                final String c = code;
                new Thread(() -> { exchange(c, verifier); s.stop(0); }, "kurospotify-auth").start();
            });
            s.start();
            String url = "https://accounts.spotify.com/authorize?response_type=code&client_id=" + enc(Cfg.I.clientId)
                + "&scope=" + enc("user-read-playback-state user-modify-playback-state")
                + "&redirect_uri=" + enc(REDIRECT) + "&code_challenge_method=S256&code_challenge=" + challenge;
            Util.getOperatingSystem().open(URI.create(url));
            status = "Tarayicida girisi onayla...";
        } catch (Exception e) { status = "Hata: " + e.getMessage(); }
    }

    static void exchange(String code, String verifier) {
        if (code == null) { status = "Giris iptal edildi"; return; }
        try {
            JsonObject o = token("grant_type=authorization_code&code=" + enc(code) + "&redirect_uri=" + enc(REDIRECT)
                + "&client_id=" + enc(Cfg.I.clientId) + "&code_verifier=" + enc(verifier));
            apply(o);
            status = loggedIn() ? "Giris yapildi" : "Giris basarisiz";
        } catch (Exception e) { status = "Hata: " + e.getMessage(); }
    }

    static JsonObject token(String body) throws Exception {
        HttpRequest rq = HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
            .header("Content-Type", "application/x-www-form-urlencoded").POST(BodyPublishers.ofString(body)).build();
        return JsonParser.parseString(H.send(rq, BodyHandlers.ofString()).body()).getAsJsonObject();
    }

    static void apply(JsonObject o) {
        if (o.has("access_token")) {
            access = o.get("access_token").getAsString();
            expires = System.currentTimeMillis() + (o.get("expires_in").getAsLong() - 30) * 1000;
        }
        if (o.has("refresh_token")) { Cfg.I.refreshToken = o.get("refresh_token").getAsString(); Cfg.save(); }
    }

    public static synchronized String get() {
        if (!loggedIn()) return null;
        if (access.isEmpty() || System.currentTimeMillis() > expires) {
            try {
                JsonObject o = token("grant_type=refresh_token&refresh_token=" + enc(Cfg.I.refreshToken)
                    + "&client_id=" + enc(Cfg.I.clientId));
                if (o.has("error") && "invalid_grant".equals(o.get("error").getAsString())) { logout(); return null; }
                apply(o);
            } catch (Exception e) { return null; }
        }
        return access.isEmpty() ? null : access;
    }

    public static void logout() {
        Cfg.I.refreshToken = ""; access = ""; expires = 0; Cfg.save(); status = "Cikis yapildi";
    }
}
