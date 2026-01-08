package net.azisaba.goldencloth.config;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ProductConfig {
    private final int dummyPrice;
    private final int basePrice;
    private final String material;
    private final String name;
    private final @Nullable Integer customModelData;
    private final List<String> lore;
    private final List<String> commands;
    private final double randomness;

    public ProductConfig(
            int dummyPrice,
            int basePrice,
            String material,
            String name,
            @Nullable Integer customModelData,
            List<String> lore,
            List<String> commands,
            double randomness
    ) {
        this.dummyPrice = dummyPrice;
        this.basePrice = basePrice;
        this.material = material;
        this.name = name;
        this.customModelData = customModelData;
        this.lore = Collections.unmodifiableList(lore);
        this.commands = Collections.unmodifiableList(commands);
        this.randomness = randomness;
    }

    public int getDummyPrice() {
        return dummyPrice;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public String getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public String getColoredName() {
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    public @Nullable Integer getCustomModelData() {
        return customModelData;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<String> getCommands() {
        return commands;
    }

    public double getRandomness() {
        return randomness;
    }


    public void execute(@NotNull Player player) {
        commands.forEach(command ->
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command.replace("<player>", player.getName()).replace("<uuid>", player.getUniqueId().toString())
            )
        );
    }
}
