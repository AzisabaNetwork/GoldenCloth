package net.azisaba.goldencloth.listener;

import net.azisaba.goldencloth.GoldenClothPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;

public class PlayerJoinListener implements Listener {
    private final GoldenClothPlugin plugin;

    public PlayerJoinListener(GoldenClothPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getDatabaseManager().getRepository().ensurePlayer(player.getUniqueId(), player.getName());
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to upsert player on join: " + e.getMessage());
            }
        });
    }
}
