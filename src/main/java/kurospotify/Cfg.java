package kurospotify;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

public class Cfg {
    public String clientId = "";
    public String refreshToken = "";
    public boolean bar = true;
    public boolean words = true;
    public int offsetMs = 0;

    public static Cfg I = new Cfg();
    static final Path P = FabricLoader.getInstance().getConfigDir().resolve("kurospotify.json");

    public static void load() {
        try {
            if (Files.exists(P)) { Cfg c = new Gson().fromJson(Files.readString(P), Cfg.class); if (c != null) I = c; }
        } catch (Exception ignored) {}
    }
    public static void save() {
        try { Files.writeString(P, new GsonBuilder().setPrettyPrinting().create().toJson(I)); } catch (Exception ignored) {}
    }
}
