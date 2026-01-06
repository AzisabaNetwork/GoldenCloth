package net.azisaba.goldencloth.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Util {
    private static final Gson GSON = new Gson();

    public static void sendDiscordWebhookAsync(@NotNull Plugin plugin, @NotNull String url, @Nullable String username, @NotNull String content) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection con = (HttpURLConnection) new URI(url).toURL().openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("User-Agent", "https://github.com/AzisabaNetwork/GoldenCloth");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                OutputStream stream = con.getOutputStream();
                JsonObject json = new JsonObject();
                if (username != null) json.addProperty("username", username);
                json.addProperty("content", content);
                stream.write(GSON.toJson(json).getBytes());
                stream.flush();
                stream.close();
                con.connect();
                InputStream errorStream = con.getErrorStream();
                if (errorStream != null) {
                    String err = new BufferedReader(new InputStreamReader(errorStream)).lines().collect(Collectors.joining("\n"));
                    plugin.getLogger().log(Level.SEVERE, "Discord webhook failed with error " + con.getResponseCode() + ": " + err);
                }
                con.getInputStream().close();
                con.disconnect();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send Discord webhook", e);
            }
        });
    }
}
