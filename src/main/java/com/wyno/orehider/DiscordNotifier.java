package com.wyno.orehider;

import org.bukkit.Bukkit;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;

public class DiscordNotifier {

    private final OreHider plugin;
    private final HttpClient httpClient;

    public DiscordNotifier(OreHider plugin) {
        this.plugin = plugin;
        // JAVA 21 OPTIMIZATION: Use the built-in HTTP Client
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void send(String message) {
        if (!plugin.getConfig().getBoolean("discord.enabled")) return;
        
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.length() < 10) return;

        // ASYNC EXECUTION
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String jsonInput = "{\"content\": \"" + escapeJson(message) + "\"}";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                        .build();

                // Non-blocking send
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                        .exceptionally(ex -> {
                            plugin.getLogger().warning("Discord Webhook Failed: " + ex.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error building Discord request", e);
            }
        });
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}