package net.azisaba.goldencloth.gui;

import net.azisaba.goldencloth.GoldenClothPlugin;
import net.azisaba.goldencloth.config.CategoryConfig;
import net.azisaba.goldencloth.config.ProductConfig;
import net.azisaba.goldencloth.db.GoldenClothRepository;
import net.azisaba.goldencloth.pricing.PriceCalculator;
import net.azisaba.goldencloth.util.ItemUtil;
import net.azisaba.goldencloth.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CustomCategoryScreen extends ShopScreen {
    private static final DecimalFormat FORMAT = new DecimalFormat("#,###.##");
    private final CategoryConfig categoryConfig;

    public CustomCategoryScreen(@NotNull GoldenClothPlugin plugin, @NotNull Player gift, @NotNull CategoryConfig categoryConfig, int index) {
        super(plugin, gift, ShopType.valueOf("Category" + (index + 2)), categoryConfig.getName());
        this.categoryConfig = categoryConfig;
        for (int i = 0; i < categoryConfig.getProducts().size(); i++) {
            ProductConfig product = categoryConfig.getProducts().get(i);
            String lorePrice;
            if (product.getBasePrice() <= 0) {
                lorePrice = "§c購入不可";
            } else if (product.getDummyPrice() == -1 || product.getDummyPrice() == product.getBasePrice() || product.getRandomness() > 0.0) {
                int actualPrice = PriceCalculator.calculateDailyPrice(product, LocalDate.now());
                lorePrice = "§6今日の価格: §a" + actualPrice + "布§7(範囲: " + (product.getBasePrice() * (1 - product.getRandomness())) + "布 ~ " + (product.getBasePrice() * (1 + product.getRandomness())) + "布)";
            } else {
                String reductionPercentage = product.getDummyPrice() == 0 ? "-∞" : FORMAT.format(100 - product.getBasePrice() / (double) product.getDummyPrice() * 100);
                lorePrice = "§6今日の価格: §8§m" + product.getDummyPrice() + "布§a " + product.getBasePrice() + "布 §6(" + reductionPercentage + "%割引)";
            }
            ItemStack stack = new ItemStack(Enum.valueOf(Material.class, product.getMaterial().toUpperCase(Locale.ROOT)));
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(product.getColoredName());
            List<String> lore = product.getLore().stream().map(s -> ChatColor.translateAlternateColorCodes('&', s)).collect(Collectors.toCollection(ArrayList::new));
            lore.add("");
            lore.add(lorePrice);
            meta.setLore(lore);
            if (product.getCustomModelData() != null) ItemUtil.setCustomModelData(meta, product.getCustomModelData());
            stack.setItemMeta(meta);
            inventory.setItem(i + 18, stack);
        }
    }

    public static class EventListener implements Listener {
        @EventHandler
        public void onInventoryDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof CustomCategoryScreen) {
                e.setCancelled(true);
            }
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if (!(e.getInventory().getHolder() instanceof CustomCategoryScreen)) {
                return;
            }
            e.setCancelled(true);
            if (e.getClickedInventory() == null || !(e.getClickedInventory().getHolder() instanceof CustomCategoryScreen)) {
                return;
            }
            CustomCategoryScreen screen = (CustomCategoryScreen) e.getClickedInventory().getHolder();
            if (screen.handle(e)) return;
            int index = e.getSlot() - 18;
            if (index < 0 || index >= screen.categoryConfig.getProducts().size()) {
                return;
            }
            ProductConfig product = screen.categoryConfig.getProducts().get(index);
            if (product.getBasePrice() <= 0) return;
            e.getWhoClicked().closeInventory();
            int actualPrice = getActualPrice(product);
            Player buyer = (Player) e.getWhoClicked();
            buyer.openInventory(new ConfirmPurchaseScreen(
                    buyer,
                    screen.gift,
                    product.getColoredName(),
                    actualPrice,
                    () -> Bukkit.getScheduler().runTaskAsynchronously(screen.plugin, () -> {
                        try {
                            GoldenClothRepository repository = screen.plugin.getDatabaseManager().getRepository();
                            GoldenClothRepository.PlayerRecord record =
                                    repository.findPlayerByUuid(screen.gift.getUniqueId());
                            if (record == null) {
                                screen.plugin.runSync(() -> {
                                    buyer.sendMessage(ChatColor.RED + "プレイヤー情報が見つかりません。");
                                    return null;
                                });
                                return;
                            }
                            boolean spent = repository.spend(
                                    record.getId(),
                                    actualPrice,
                                    "shop",
                                    product.getName()
                            );
                            screen.plugin.runSync(() -> {
                                if (spent) {
                                    product.execute(screen.gift);
                                    buyer.sendMessage(ChatColor.GREEN + "購入しました！");
                                    Util.sendDiscordWebhookAsync(screen.plugin, screen.plugin.getConfig().getString("discordWebhookNotifyUrl"), null,
                                            e.getWhoClicked().getName() + " (" + e.getWhoClicked().getUniqueId() + ")が" + screen.gift.getName() + " (" + screen.gift.getUniqueId() + ")に" + product.getColoredName() + "を" + actualPrice + "布で購入しました");
                                    if (!screen.gift.getUniqueId().equals(e.getWhoClicked().getUniqueId())) {
                                        Bukkit.broadcastMessage("§a§l" + e.getWhoClicked().getName() + "さんが" + screen.gift.getName() + "さんに§r§f" + product.getColoredName() + "§a§lを購入しました！");
                                    }
                                } else {
                                    buyer.sendMessage(ChatColor.RED + "残高が不足しています。");
                                }
                                return null;
                            });
                        } catch (SQLException ex) {
                            screen.plugin.runSync(() -> {
                                buyer.sendMessage(ChatColor.RED + "購入処理中にエラーが発生しました。");
                                return null;
                            });
                            ex.printStackTrace();
                        }
                    }),
                    () -> buyer.openInventory(screen.getInventory())
            ).getInventory());
        }
    }

    private static int getActualPrice(ProductConfig product) {
        if (product.getRandomness() > 0.0) {
            return PriceCalculator.calculateDailyPrice(product, LocalDate.now());
        }
        return product.getBasePrice();
    }
}
