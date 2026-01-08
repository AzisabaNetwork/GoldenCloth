package net.azisaba.goldencloth.gui;

import net.azisaba.azipluginmessaging.api.AziPluginMessagingProvider;
import net.azisaba.azipluginmessaging.api.protocol.Protocol;
import net.azisaba.azipluginmessaging.api.protocol.message.PlayerMessage;
import net.azisaba.azipluginmessaging.api.protocol.message.ProxyboundGiveNitroSaraMessage;
import net.azisaba.azipluginmessaging.api.protocol.message.ProxyboundGiveSaraMessage;
import net.azisaba.goldencloth.GoldenClothPlugin;
import net.azisaba.goldencloth.db.GoldenClothRepository;
import net.azisaba.goldencloth.pricing.PriceCalculator;
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
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class RankScreen extends ShopScreen {
    private static final double RANDOMNESS = 0.3;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Map<@Nullable String, List<String>> benefits = new HashMap<>();
    private static final Map<@NotNull Integer, SlotData> slots = new HashMap<>();

    static {
        benefits.put("100yen", Arrays.asList(
                "§7アジ鯖に100円寄付したものに送られる称号。",
                "",
                "§6§n全鯖共通",
                "§f- §e名前の最初に§8[§1100円皿§8]§eがつき、名前も同じ色になる",
                "",
                "§6§nLGW",
                "§f- §1100円HANABI§eがもらえる",
                "§f- §d寄付ガチャチケット§eが1枚もらえる",
                "§f- §b/h§eコマンドで100円皿専用のパーティクルがアクセス可能になる"
        ));
        benefits.put("500yen", Arrays.asList(
                "§7アジ鯖に500円寄付したものに送られる称号。",
                "",
                "§6§n全鯖共通",
                "§f- §e名前の最初に§8[§b500円皿§8]§eがつき、名前も同じ色になる",
                "",
                "§6§nLGW",
                "§f- §b500円HANABI§eがもらえる",
                "§f- §d寄付ガチャチケット§eが3枚もらえる",
                "§f- §b/h§eコマンドで500円皿専用のパーティクルがアクセス可能になる"
        ));
        benefits.put("1000yen", Arrays.asList(
                "§7アジ鯖に1000円寄付したものに送られる称号。",
                "",
                "§6§n全鯖共通",
                "§f- §e名前の最初に§8[§a1000円皿§8]§eがつき、名前も同じ色になる",
                "",
                "§6§nLGW",
                "§f- §a1000円HANABI§eがもらえる",
                "§f- §d寄付ガチャチケット§eが5枚もらえる",
                "§f- §b/h§eコマンドで1000円皿専用のパーティクルがアクセス可能になる",
                "§f- §b/hat§eコマンドが使用可能になる(アイテムを頭にかぶることができる)"
        ));
        benefits.put("2000yen", Arrays.asList(
                "§7アジ鯖に2000円寄付したものに送られる称号。",
                "",
                "§6§n全鯖共通",
                "§f- §e名前の最初に§8[§d2000円皿§8]§eがつき、名前も同じ色になる",
                "",
                "§6§nLGW",
                "§f- §d2000円HANABI§eがもらえる",
                "§f- §d寄付ガチャチケット§eが10枚もらえる",
                "§f- §b/h§eコマンドで2000円皿専用のパーティクルがアクセス可能になる",
                "§f- §b/hat§eコマンドが使用可能になる(アイテムを頭にかぶることができる)"
        ));
        benefits.put("5000yen", Arrays.asList(
                "§7アジ鯖に5000円寄付したものに送られる称号。",
                "",
                "§6§n全鯖共通",
                "§f- §e名前の最初に§8[§55000円皿§8]§eがつき、名前も同じ色になる",
                "§f- §eチャットでカラーコードと装飾コードが使えるようになる",
                "",
                "§6§nLGW",
                "§f- §55000円HANABI§eがもらえる",
                "§f- §d寄付ガチャチケット§eが20枚もらえる",
                "§f- §b/h§eコマンドで5000円皿専用のパーティクルがアクセス可能になる",
                "§f- §b/hat§eコマンドが使用可能になる(アイテムを頭にかぶることができる)",
                "§f- §6DOROKUN§eがもらえる"
        ));
        benefits.put("10000yen", Arrays.asList(
                "§7アジ鯖に10000円寄付したものに送られる称号。",
                "",
                "§6§n全鯖共通",
                "§f- §e名前の最初に§8[§610000円皿§8]§eがつき、名前も同じ色になる",
                "§f- §eチャットでカラーコードと装飾コードが使えるようになる",
                "",
                "§6§nLGW",
                "§f- §610000円HANABI§eがもらえる",
                "§f- §d寄付ガチャチケット§eが32枚もらえる",
                "§f- §b/h§eコマンドで5000円皿専用のパーティクルがアクセス可能になる",
                "§f- §b/hat§eコマンドが使用可能になる(アイテムを頭にかぶることができる)",
                "§f- §6カボチャ爆弾§eがもらえる"
        ));
        benefits.put("50000yen", Arrays.asList(
                "§7アジ鯖に50000円寄付したものに送られる称号。",
                "",
                "§6§n全鯖共通",
                "§f- §e名前の最初に§8[§25§a0§20§a0§20§a円§2皿§8]§eがつき、名前が§2緑色§eになる",
                "§f- §eチャットでカラーコードと装飾コードが使えるようになる",
                "",
                "§6§nLGW",
                "§f- §d寄付ガチャチケット§eが128枚もらえる",
                "§f- §b/h§eコマンドで50000円皿専用のパーティクルがアクセス可能になる",
                "§f- §b/hat§eコマンドが使用可能になる(アイテムを頭にかぶることができる)"
        ));
        benefits.put("changegamingsara", Arrays.asList(
                "§7アジ鯖に人生を捧げるゲーマーに送られる称号。",
                "",
                "§6§n全鯖共通",
                "§f- §e名前の最初に§8[§9§lゲ§2§lー§a§lミ§e§lン§4§lグ§8]§eがつき、名前が§b水色§eになる",
                "§f- §eチャットでカラーコードと装飾コードが使えるようになる",
                "§f- §b/gamingsara§eでゲーミングランクの表示",
                "",
                "§6§nLife",
                "§f- §eニックネームでカラーコードと日本語が使用可能になる"
        ));
        benefits.put(null, Arrays.asList(
                "§7アジ鯖に毎月寄付している者に送られる称号。",
                "",
                "§6§n全鯖共通",
                "§f- §e名前の最初に§8[§3Nitro§6§l⚡§8]§eがつき、名前も同じ色になる",
                "§f- §eチャットでカラーコードと装飾コードが使えるようになる",
                "§f- §b/togglenitro§eでランクの表示/非表示の切り替え",
                "§f- §eほぼすべてのサーバーで名前の最初に着くPrefixを自由に変更できるようになる",
                " §f §b/setprefix (新しいprefix)§eで設定、§b/clearprefix§eで削除 (globalよりもこちらが優先)",
                " §f §b/setglobalprefix (新しいprefix)§eで設定、§b/clearglobalprefix§eで削除",
                "§f- §eギルドチャットの公開設定",
                " §f §b/guild open§eで§b/guild join (ギルド名)§eで招待無しで参加可能になる",
                "§f- §bbeta.azisaba.net§eの接続権",
                " §f §e今後ベータ版のサーバーが先行公開する際に参加可能になります。",
                "",
                "§6§nロビー",
                "§f- §eロビーサーバーで飛行できるようになる",
                "§f- §eロビーのログインメッセージ変更(固定)",
                "",
                "§6§nLife",
                "§f- §eニックネームでカラーコードと日本語が使用可能になる",
                "",
                "§d※継続課金ではなく、30日が追加される形となります。"
        ));

        slots.put(28, new SlotData(Material.DIAMOND, 100 * 4, "§1100円皿", "100yen", p -> {
            net.azisaba.azipluginmessaging.api.entity.Player player = AziPluginMessagingProvider.get().getPlayerAdapter(Player.class).get(p);
            if (!Protocol.P_GIVE_SARA.sendPacket(AziPluginMessagingProvider.get().getServer().getPacketSender(), new ProxyboundGiveSaraMessage(100, player))) {
                throw new RuntimeException("Failed to send packet");
            }
        }));
        slots.put(29, new SlotData(Material.DIAMOND, 500 * 4, "§b500円皿", "500yen", p -> {
            net.azisaba.azipluginmessaging.api.entity.Player player = AziPluginMessagingProvider.get().getPlayerAdapter(Player.class).get(p);
            if (!Protocol.P_GIVE_SARA.sendPacket(AziPluginMessagingProvider.get().getServer().getPacketSender(), new ProxyboundGiveSaraMessage(500, player))) {
                throw new RuntimeException("Failed to send packet");
            }
        }));
        slots.put(30, new SlotData(Material.DIAMOND, 1000 * 4, "§a1000円皿", "1000yen", p -> {
            net.azisaba.azipluginmessaging.api.entity.Player player = AziPluginMessagingProvider.get().getPlayerAdapter(Player.class).get(p);
            if (!Protocol.P_GIVE_SARA.sendPacket(AziPluginMessagingProvider.get().getServer().getPacketSender(), new ProxyboundGiveSaraMessage(1000, player))) {
                throw new RuntimeException("Failed to send packet");
            }
        }));
        slots.put(31, new SlotData(Material.DIAMOND, 2000 * 4, "§d2000円皿", "2000yen", p -> {
            net.azisaba.azipluginmessaging.api.entity.Player player = AziPluginMessagingProvider.get().getPlayerAdapter(Player.class).get(p);
            if (!Protocol.P_GIVE_SARA.sendPacket(AziPluginMessagingProvider.get().getServer().getPacketSender(), new ProxyboundGiveSaraMessage(2000, player))) {
                throw new RuntimeException("Failed to send packet");
            }
        }));
        slots.put(32, new SlotData(Material.DIAMOND, 5000 * 4, "§55000円皿", "5000yen", p -> {
            net.azisaba.azipluginmessaging.api.entity.Player player = AziPluginMessagingProvider.get().getPlayerAdapter(Player.class).get(p);
            if (!Protocol.P_GIVE_SARA.sendPacket(AziPluginMessagingProvider.get().getServer().getPacketSender(), new ProxyboundGiveSaraMessage(5000, player))) {
                throw new RuntimeException("Failed to send packet");
            }
        }));
        slots.put(33, new SlotData(Material.DIAMOND, 10000 * 4, "§610000円皿", "10000yen", p -> {
            net.azisaba.azipluginmessaging.api.entity.Player player = AziPluginMessagingProvider.get().getPlayerAdapter(Player.class).get(p);
            if (!Protocol.P_GIVE_SARA.sendPacket(AziPluginMessagingProvider.get().getServer().getPacketSender(), new ProxyboundGiveSaraMessage(10000, player))) {
                throw new RuntimeException("Failed to send packet");
            }
        }));
        slots.put(34, new SlotData(Material.DIAMOND, 50000 * 4, "§25§a0§20§a0§20§a円§2皿", "50000yen", p -> {
            net.azisaba.azipluginmessaging.api.entity.Player player = AziPluginMessagingProvider.get().getPlayerAdapter(Player.class).get(p);
            if (!Protocol.P_GIVE_SARA.sendPacket(AziPluginMessagingProvider.get().getServer().getPacketSender(), new ProxyboundGiveSaraMessage(50000, player))) {
                throw new RuntimeException("Failed to send packet");
            }
        }));
        slots.put(38, new SlotData(Material.EMERALD, 3000 * 4, "§9§lゲ§2§lー§a§lミ§e§lン§4§lグ§eランク", "changegamingsara", p -> {
            net.azisaba.azipluginmessaging.api.entity.Player player = AziPluginMessagingProvider.get().getPlayerAdapter(Player.class).get(p);
            if (!Protocol.P_GIVE_GAMING_SARA.sendPacket(AziPluginMessagingProvider.get().getServer().getPacketSender(), new PlayerMessage(player))) {
                throw new RuntimeException("Failed to send packet");
            }
        }));
        slots.put(42, new SlotData(Material.EMERALD, 500 * 4, "§3Nitro§6§l⚡ §e(30日間)", null, p -> {
            net.azisaba.azipluginmessaging.api.entity.Player player = AziPluginMessagingProvider.get().getPlayerAdapter(Player.class).get(p);
            if (!Protocol.P_GIVE_NITRO_SARA.sendPacket(AziPluginMessagingProvider.get().getServer().getPacketSender(), new ProxyboundGiveNitroSaraMessage(player, 30, TimeUnit.DAYS))) {
                throw new RuntimeException("Failed to send packet");
            }
        }));
    }

    public RankScreen(@NotNull GoldenClothPlugin plugin, @NotNull Player gift) {
        super(plugin, gift, ShopType.Rank, "ランク");
        slots.forEach((slot, data) -> {
            List<String> benefit = benefits.getOrDefault(data.groupName, Collections.emptyList());
            int actualPrice = data.getActualPrice(gift);
            String lorePrice;
            if (actualPrice <= 0) {
                lorePrice = "§c購入不可";
            } else if (data.groupName != null && gift.hasPermission("group." + data.groupName)) {
                lorePrice = "§c購入済み";
            } else if (data.price == actualPrice || RANDOMNESS > 0.0) {
                lorePrice = "§6今日の価格: §a" + actualPrice + "布 §7(範囲: " + (data.price * (1 - RANDOMNESS)) + "布 ~ " + (data.price * (1 + RANDOMNESS)) + "布)";
            } else {
                lorePrice = "§6今日の価格: §8§m" + data.price + "布§a " + actualPrice + "布 §7(範囲: " + (data.price * (1 - RANDOMNESS)) + "布 ~ " + (data.price * (1 + RANDOMNESS)) + "布)";
            }
            ItemStack stack = new ItemStack(data.type);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(data.name);
            List<String> lore = new ArrayList<>(benefit);
            lore.add("");
            lore.add(lorePrice);
            meta.setLore(lore);
            stack.setItemMeta(meta);
            inventory.setItem(slot, stack);
        });
        ItemStack stack = new ItemStack(Material.REDSTONE_TORCH_ON);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("§6残高");
        meta.setLore(Collections.singletonList("§7読み込み中..."));
        stack.setItemMeta(meta);
        inventory.setItem(53, stack);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                GoldenClothRepository repository = plugin.getDatabaseManager().getRepository();
                GoldenClothRepository.PlayerRecord record = repository.findPlayerByUuid(gift.getUniqueId());
                if (record == null) {
                    updateBalanceLore(Collections.singletonList("§cプレイヤー情報が見つかりません。"));
                    return;
                }
                List<GoldenClothRepository.LotBalance> lots =
                        repository.getActiveLots(record.getId(), Instant.now());
                int total = 0;
                List<String> lore = new ArrayList<>();
                for (GoldenClothRepository.LotBalance lot : lots) {
                    total += lot.getRemaining();
                }
                lore.add("§a合計: " + total + "布");
                if (lots.isEmpty()) {
                    lore.add("§7残高がありません。");
                } else {
                    ZoneId zone = ZoneId.systemDefault();
                    for (GoldenClothRepository.LotBalance lot : lots) {
                        String date = DATE_FORMAT.format(lot.getExpiresAt().atZone(zone).toLocalDate());
                        lore.add("§7- " + lot.getRemaining() + "布 §8(期限: " + date + ")");
                    }
                }
                updateBalanceLore(lore);
            } catch (SQLException e) {
                updateBalanceLore(Collections.singletonList("§c残高の取得に失敗しました。"));
                e.printStackTrace();
            }
        });
    }

    private void updateBalanceLore(List<String> lore) {
        plugin.runSync(() -> {
            ItemStack item = inventory.getItem(53);
            if (item == null || item.getType() == Material.AIR) {
                item = new ItemStack(Material.REDSTONE_TORCH_ON);
            }
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§6残高");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(53, item);
            return null;
        });
    }
    
    public static class EventListener implements Listener {
        private final GoldenClothPlugin plugin;
        
        public EventListener(GoldenClothPlugin plugin) {
            this.plugin = plugin;
        }
        
        @EventHandler
        public void onInventoryDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof RankScreen) {
                e.setCancelled(true);
            }
        }
        
        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if (!(e.getInventory().getHolder() instanceof RankScreen)) {
                return;
            }
            e.setCancelled(true);
            if (e.getClickedInventory() == null || !(e.getClickedInventory().getHolder() instanceof RankScreen)) {
                return;
            }
            RankScreen screen = (RankScreen) e.getClickedInventory().getHolder();
            if (screen.handle(e)) return;
            SlotData data = slots.get(e.getSlot());
            if (data == null) return;
            int actualPrice = data.getActualPrice(screen.gift);
            if (actualPrice > data.price) throw new IllegalArgumentException("actualPrice must be less than or equal to price (" + actualPrice + " > " + data.price + ")");
            if (actualPrice <= 0 || (data.groupName != null && screen.gift.hasPermission("group." + data.groupName))) {
                e.getWhoClicked().sendMessage(ChatColor.RED + "この商品はすでに購入済みです！");
                return;
            }
            e.getWhoClicked().closeInventory();
            Player buyer = (Player) e.getWhoClicked();
            buyer.openInventory(new ConfirmPurchaseScreen(
                    buyer,
                    screen.gift,
                    data.name,
                    actualPrice,
                    () -> Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        try {
                            GoldenClothRepository repository = plugin.getDatabaseManager().getRepository();
                            GoldenClothRepository.PlayerRecord record =
                                    repository.findPlayerByUuid(screen.gift.getUniqueId());
                            if (record == null) {
                                plugin.runSync(() -> {
                                    buyer.sendMessage(ChatColor.RED + "プレイヤー情報が見つかりません。");
                                    return null;
                                });
                                return;
                            }
                            boolean spent = repository.spend(
                                    record.getId(),
                                    actualPrice,
                                    "rank",
                                    data.name
                            );
                            plugin.runSync(() -> {
                                if (spent) {
                                    data.action.accept(screen.gift);
                                    Util.sendDiscordWebhookAsync(plugin, plugin.getConfig().getString("discordWebhookNotifyUrl"), null,
                                            e.getWhoClicked().getName() + " (" + e.getWhoClicked().getUniqueId() + ")が" + screen.gift.getName() + " (" + screen.gift.getUniqueId() + ")に" + data.name + "を" + actualPrice + "布で購入しました");
                                    buyer.sendMessage(ChatColor.GREEN + "購入しました！");
                                    if (!screen.gift.getUniqueId().equals(e.getWhoClicked().getUniqueId())) {
                                        Bukkit.broadcastMessage("§a§l" + e.getWhoClicked().getName() + "さんが" + screen.gift.getName() + "さんに§r§f" + data.name + "§a§lを購入しました！");
                                    }
                                } else {
                                    buyer.sendMessage(ChatColor.RED + "残高が不足しています。");
                                }
                                return null;
                            });
                        } catch (SQLException ex) {
                            plugin.runSync(() -> {
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

    public static int getSaraPriceReduction(@NotNull Player player) {
        if (player.hasPermission("group.100000yen")) {
            return 100000 * 4;
        } else if (player.hasPermission("group.50000yen")) {
            return 50000 * 4;
        } else if (player.hasPermission("group.10000yen")) {
            return 10000 * 4;
        } else if (player.hasPermission("group.5000yen")) {
            return 5000 * 4;
        } else if (player.hasPermission("group.2000yen")) {
            return 2000 * 4;
        } else if (player.hasPermission("group.1000yen")) {
            return 1000 * 4;
        } else if (player.hasPermission("group.500yen")) {
            return 500 * 4;
        } else if (player.hasPermission("group.100yen")) {
            return 100 * 4;
        }
        return 0;
    }

    public static class SlotData {
        public final @NotNull Material type;
        public final int price;
        public final @NotNull String name;
        public final @Nullable String groupName;
        public final @NotNull Consumer<Player> action;

        public SlotData(@NotNull Material type, int price, @NotNull String name, @Nullable String groupName, @NotNull Consumer<Player> action) {
            this.type = type;
            this.price = price;
            this.name = name;
            this.groupName = groupName;
            this.action = action;
        }

        public int getActualPrice(@NotNull Player player) {
            int price = this.price;
            if (type == Material.DIAMOND) {
                price -= getSaraPriceReduction(player);
            }
            price = PriceCalculator.calculateDailyPrice(price, RANDOMNESS, LocalDate.now(), name, type.name());
            return price;
        }
    }
}
