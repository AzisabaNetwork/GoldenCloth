package net.azisaba.goldencloth.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static GoldenClothConfig load(FileConfiguration config, Logger logger) {
        String discordWebhookNotifyUrl = config.getString("discordWebhookNotifyUrl", "");
        DatabaseConfig database = loadDatabase(config);
        List<CategoryConfig> categories = new ArrayList<>();

        for (Map<?, ?> categoryMap : asMapList(config.get("categories"))) {
            String categoryName = asString(categoryMap.get("name"), "");
            String categoryMaterial = asString(categoryMap.get("material"), "");
            List<ProductConfig> products = new ArrayList<>();

            for (Map<?, ?> productMap : asMapList(categoryMap.get("products"))) {
                int basePrice = asInt(productMap.get("basePrice"), 0);
                String productMaterial = asString(productMap.get("material"), "");
                String productName = asString(productMap.get("name"), "");
                List<String> lore = asStringList(productMap.get("lore"));
                List<String> commands = asStringList(productMap.get("commands"));
                double randomness = asDouble(productMap.get("randomness"), 0.0);
                Integer customModelData = asInteger(productMap.get("customModelData"), null);

                if (randomness < 0.0 || randomness > 1.0) {
                    logger.warning("Product randomness must be between 0 and 1. Clamping value: " + randomness);
                    randomness = Math.max(0.0, Math.min(1.0, randomness));
                }

                products.add(new ProductConfig(
                        basePrice,
                        productMaterial,
                        productName,
                        customModelData,
                        lore,
                        commands,
                        randomness
                ));
            }

            categories.add(new CategoryConfig(categoryName, categoryMaterial, products));
        }

        return new GoldenClothConfig(discordWebhookNotifyUrl, database, categories);
    }

    private static DatabaseConfig loadDatabase(FileConfiguration config) {
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String scheme = config.getString("database.scheme", "");
        String username = config.getString("database.username", "");
        String password = config.getString("database.password", "");
        boolean useSSL = config.getBoolean("database.useSSL", false);
        return new DatabaseConfig(host, port, scheme, username, password, useSSL);
    }

    private static List<Map<?, ?>> asMapList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<Map<?, ?>> result = new ArrayList<>();
        for (Object element : (List<?>) value) {
            if (element instanceof Map) {
                result.add((Map<?, ?>) element);
            }
        }
        return result;
    }

    private static List<String> asStringList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object element : (List<?>) value) {
            if (element != null) {
                result.add(String.valueOf(element));
            }
        }
        return result;
    }

    private static String asString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private static int asInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static int asInteger(Object value, Integer defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static double asDouble(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
