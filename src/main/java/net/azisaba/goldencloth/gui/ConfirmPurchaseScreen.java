package net.azisaba.goldencloth.gui;

import net.azisaba.goldencloth.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfirmPurchaseScreen implements InventoryHolder {
    private static final int CANCEL_SLOT = 11;
    private static final int CONFIRM_SLOT = 15;

    private final Player player;
    private final Runnable confirmAction;
    private final Runnable cancelAction;
    private final Inventory inventory;

    public ConfirmPurchaseScreen(
            @NotNull Player player,
            @NotNull Player gift,
            @NotNull String itemName,
            int price,
            @NotNull Runnable confirmAction,
            @NotNull Runnable cancelAction
    ) {
        this.player = player;
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
        this.inventory = Bukkit.createInventory(this, 27, "購入確認");

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(itemName);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "価格: " + ChatColor.GREEN + price + "布");
        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        inventory.setItem(13, info);

        inventory.setItem(CANCEL_SLOT, ItemUtil.itemOf(Material.WOOL, 1, (short) 14, "§cキャンセル"));
        if (!player.getUniqueId().equals(gift.getUniqueId())) {
            inventory.setItem(CONFIRM_SLOT, ItemUtil.itemOf(Material.WOOL, 1, (short) 5, "§a" + gift.getName() + "さんに贈る"));
        } else {
            inventory.setItem(CONFIRM_SLOT, ItemUtil.itemOf(Material.WOOL, 1, (short) 5, "§a購入する"));
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private void confirm() {
        confirmAction.run();
    }

    private void cancel() {
        cancelAction.run();
    }

    public static class EventListener implements Listener {
        @EventHandler
        public void onInventoryDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof ConfirmPurchaseScreen) {
                e.setCancelled(true);
            }
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if (!(e.getInventory().getHolder() instanceof ConfirmPurchaseScreen)) {
                return;
            }
            e.setCancelled(true);
            if (e.getClickedInventory() == null || !(e.getClickedInventory().getHolder() instanceof ConfirmPurchaseScreen)) {
                return;
            }
            ConfirmPurchaseScreen screen = (ConfirmPurchaseScreen) e.getClickedInventory().getHolder();
            if (e.getSlot() == CONFIRM_SLOT) {
                screen.player.closeInventory();
                screen.confirm();
            } else if (e.getSlot() == CANCEL_SLOT) {
                screen.player.closeInventory();
                screen.cancel();
            }
        }
    }
}
