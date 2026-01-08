package net.azisaba.goldencloth;

import net.azisaba.goldencloth.command.ClothCommand;
import net.azisaba.goldencloth.config.ConfigLoader;
import net.azisaba.goldencloth.config.GoldenClothConfig;
import net.azisaba.goldencloth.command.GoldenClothCommand;
import net.azisaba.goldencloth.db.DatabaseManager;
import net.azisaba.goldencloth.gui.ConfirmPurchaseScreen;
import net.azisaba.goldencloth.gui.CustomCategoryScreen;
import net.azisaba.goldencloth.gui.RankScreen;
import net.azisaba.goldencloth.listener.PlayerJoinListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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
        Objects.requireNonNull(getCommand("goldencloth")).setExecutor(new GoldenClothCommand(this));
        Objects.requireNonNull(getCommand("cloth")).setExecutor(new ClothCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new RankScreen.EventListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomCategoryScreen.EventListener(), this);
        getServer().getPluginManager().registerEvents(new ConfirmPurchaseScreen.EventListener(), this);
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

    public <R> @NotNull CompletableFuture<R> runSync(@NotNull Supplier<R> action) {
        Objects.requireNonNull(action, "action");
        CompletableFuture<R> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                future.complete(action.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
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
