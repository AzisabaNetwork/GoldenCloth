package net.azisaba.goldencloth.command;

import net.azisaba.goldencloth.GoldenClothPlugin;
import net.azisaba.goldencloth.gui.RankScreen;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public class ClothCommand implements TabExecutor {
    private final GoldenClothPlugin plugin;

    public ClothCommand(GoldenClothPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 1 && args[0].equals("yes")) {
            ((Player) sender).openInventory(new RankScreen(plugin, (Player) sender, (Player) sender).getInventory());
        } else if (args.length == 1) {
            Player player = Bukkit.getPlayerExact(args[0]);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "プレイヤーが見つかりません: " + args[0]);
                return true;
            }
            ((Player) sender).openInventory(new RankScreen(plugin, player, (Player) sender).getInventory());
        } else {
            sender.sendMessage("§b§nhttps://link.azisaba.net/sct");
            sender.sendMessage("§e続行する前に上記のURLを確認してください。");
            TextComponent component = new TextComponent("✔確認したので続行する");
            component.setColor(ChatColor.GREEN);
            component.setUnderlined(true);
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("購入画面を表示する")));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cloth yes"));
            sender.spigot().sendMessage(component);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}
