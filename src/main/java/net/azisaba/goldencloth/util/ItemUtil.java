package net.azisaba.goldencloth.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ItemUtil {
    public static ItemStack itemOf(Material type, int amount, short durability, @Nullable String displayName, @Nullable String... lore) {
        ItemStack stack = new ItemStack(type, amount);
        stack.setDurability(durability);
        ItemMeta meta = stack.getItemMeta();
        if (displayName != null) meta.setDisplayName(displayName);
        if (lore != null && lore.length > 0) meta.setLore(Arrays.asList(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    public static void setCustomModelData(@NotNull ItemMeta meta, Integer data) {
        try {
            ItemMeta.class.getMethod("setCustomModelData", Integer.class).invoke(meta, data);
        } catch (ReflectiveOperationException ignored) {}
    }
}
