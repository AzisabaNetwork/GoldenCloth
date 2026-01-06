package net.azisaba.goldencloth.config;

import java.util.Collections;
import java.util.List;

public class GoldenClothConfig {
    private final String discordWebhookNotifyUrl;
    private final DatabaseConfig database;
    private final List<CategoryConfig> categories;

    public GoldenClothConfig(String discordWebhookNotifyUrl, DatabaseConfig database, List<CategoryConfig> categories) {
        this.discordWebhookNotifyUrl = discordWebhookNotifyUrl;
        this.database = database;
        this.categories = Collections.unmodifiableList(categories);
    }

    public String getDiscordWebhookNotifyUrl() {
        return discordWebhookNotifyUrl;
    }

    public DatabaseConfig getDatabase() {
        return database;
    }

    public List<CategoryConfig> getCategories() {
        return categories;
    }
}
