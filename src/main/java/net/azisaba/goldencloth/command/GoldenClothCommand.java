package net.azisaba.goldencloth.command;

import net.azisaba.goldencloth.GoldenClothPlugin;
import net.azisaba.goldencloth.db.GoldenClothRepository;
import net.azisaba.goldencloth.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

public class GoldenClothCommand implements CommandExecutor {
    private final GoldenClothPlugin plugin;

    public GoldenClothCommand(GoldenClothPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3 || !"addPurchase".equalsIgnoreCase(args[0])) {
            sender.sendMessage("Usage: /" + label + " addPurchase <player> <amount> [note]");
            return true;
        }

        if (!sender.hasPermission("goldencloth.admin")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        String target = args[1];
        Integer amount = parsePositiveInt(args[2]);
        if (amount == null) {
            sender.sendMessage("Amount must be a positive integer.");
            return true;
        }

        String note = null;
        if (args.length > 3) {
            note = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(90, ChronoUnit.DAYS);
        String finalNote = note;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                GoldenClothRepository repository = plugin.getDatabaseManager().getRepository();
                GoldenClothRepository.PlayerRecord player = resolvePlayer(repository, target);
                if (player == null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage("Player not found in database: " + target)
                    );
                    return;
                }
                long playerId = player.getId();
                repository.addPurchase(playerId, amount, now, expiresAt, finalNote);
                Util.sendDiscordWebhookAsync(plugin, plugin.getConfig().getString("discordWebhookNotifyUrl"), null, player.getName() + " (" + player.getUuid() + ")が" + amount + "布を購入しました");
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage("Added purchase of " + amount + " for " + player.getName() + ".")
                );
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage("Failed to add purchase: " + e.getMessage())
                );
            }
        });

        return true;
    }

    private GoldenClothRepository.PlayerRecord resolvePlayer(GoldenClothRepository repository, String target)
            throws SQLException {
        try {
            UUID uuid = UUID.fromString(target);
            return repository.findPlayerByUuid(uuid);
        } catch (IllegalArgumentException ignored) {
            return repository.findPlayerByName(target);
        }
    }

    private Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
