package net.azisaba.goldencloth;

import net.azisaba.goldencloth.config.ConfigLoader;
import net.azisaba.goldencloth.config.GoldenClothConfig;
import net.azisaba.goldencloth.command.GoldenClothCommand;
import net.azisaba.goldencloth.db.DatabaseManager;
import net.azisaba.goldencloth.listener.PlayerJoinListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.ZonedDateTime;

public class GoldenClothPlugin extends JavaPlugin {
    private GoldenClothConfig goldenClothConfig;
    private DatabaseManager databaseManager;
    private int expirationTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        goldenClothConfig = ConfigLoader.load(getConfig(), getLogger());
        databaseManager = new DatabaseManager(goldenClothConfig.getDatabase(), getLogger());
        databaseManager.initialize();
        if (getCommand("goldencloth") != null) {
            getCommand("goldencloth").setExecutor(new GoldenClothCommand(this));
        }
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        scheduleExpirationTask();
    }

    @Override
    public void onDisable() {
        if (expirationTaskId != -1) {
            getServer().getScheduler().cancelTask(expirationTaskId);
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public GoldenClothConfig getGoldenClothConfig() {
        return goldenClothConfig;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    private void scheduleExpirationTask() {
        long delayTicks = ticksUntilNextMidnight();
        long periodTicks = Duration.ofDays(1).getSeconds() * 20L;
        expirationTaskId = getServer().getScheduler().runTaskTimerAsynchronously(
                this,
                () -> databaseManager.getRepository().expireLotsSafely(getLogger()),
                delayTicks,
                periodTicks
        ).getTaskId();
    }

    private long ticksUntilNextMidnight() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.getZone());
        long seconds = Duration.between(now, nextMidnight).getSeconds();
        if (seconds < 1) {
            seconds = 1;
        }
        return seconds * 20L;
    }
}
