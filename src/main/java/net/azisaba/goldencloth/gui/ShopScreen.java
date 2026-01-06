package net.azisaba.goldencloth.gui;

import net.azisaba.goldencloth.GoldenClothPlugin;
import net.azisaba.goldencloth.config.CategoryConfig;
import net.azisaba.goldencloth.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class ShopScreen implements InventoryHolder {
    protected final GoldenClothPlugin plugin;
    protected final Inventory inventory;

    public ShopScreen(@NotNull GoldenClothPlugin plugin, @NotNull ShopType type, @NotNull String title) {
        this.plugin = plugin;
        inventory = Bukkit.createInventory(this, 54, title);
        inventory.setItem(0, ItemUtil.itemOf(Material.EMERALD, 1, (short) 0, "§aランク"));
        for (int i = 0; i < plugin.getGoldenClothConfig().getCategories().size(); i++) {
            if (i < 8) {
                CategoryConfig category = plugin.getGoldenClothConfig().getCategories().get(i);
                inventory.setItem(i + 1, ItemUtil.itemOf(Material.valueOf(category.getMaterial().toUpperCase(Locale.ROOT)), 1, (short) 0, "§a" + ChatColor.translateAlternateColorCodes('&', category.getName())));
            }
        }
        inventory.setItem(9, ItemUtil.itemOf(Material.STAINED_GLASS_PANE, 1, (short) (type == ShopType.Rank ? 4 : 15), " "));
        inventory.setItem(10, ItemUtil.itemOf(Material.STAINED_GLASS_PANE, 1, (short) (type == ShopType.Category2 ? 4 : 15), " "));
        inventory.setItem(11, ItemUtil.itemOf(Material.STAINED_GLASS_PANE, 1, (short) (type == ShopType.Category3 ? 4 : 15), " "));
        inventory.setItem(12, ItemUtil.itemOf(Material.STAINED_GLASS_PANE, 1, (short) (type == ShopType.Category4 ? 4 : 15), " "));
        inventory.setItem(13, ItemUtil.itemOf(Material.STAINED_GLASS_PANE, 1, (short) (type == ShopType.Category5 ? 4 : 15), " "));
        inventory.setItem(14, ItemUtil.itemOf(Material.STAINED_GLASS_PANE, 1, (short) (type == ShopType.Category6 ? 4 : 15), " "));
        inventory.setItem(15, ItemUtil.itemOf(Material.STAINED_GLASS_PANE, 1, (short) (type == ShopType.Category7 ? 4 : 15), " "));
        inventory.setItem(16, ItemUtil.itemOf(Material.STAINED_GLASS_PANE, 1, (short) (type == ShopType.Category8 ? 4 : 15), " "));
        inventory.setItem(17, ItemUtil.itemOf(Material.STAINED_GLASS_PANE, 1, (short) (type == ShopType.Category9 ? 4 : 15), " "));
    }

    protected boolean handle(@NotNull InventoryClickEvent e) {
        if (e.getSlot() == 0) {
            //e.getWhoClicked().openInventory();
            return true;
        }
        if (e.getSlot() <= 8) {
            List<CategoryConfig> categories = plugin.getGoldenClothConfig().getCategories();
            if (categories.size() <= e.getSlot()) {
                //e.getWhoClicked().openInventory();
                return true;
            }
        }
        if (e.getWhoClicked().hasPermission("goldencloth.buy")) {
            e.getWhoClicked().closeInventory();
            e.getWhoClicked().sendMessage(ChatColor.RED + "商品を購入する権限がありません！");
            return true;
        }
        return false;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public enum ShopType {
        Rank,
        Category2,
        Category3,
        Category4,
        Category5,
        Category6,
        Category7,
        Category8,
        Category9,
    }
}
