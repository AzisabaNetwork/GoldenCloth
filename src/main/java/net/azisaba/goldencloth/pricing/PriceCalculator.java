package net.azisaba.goldencloth.pricing;

import net.azisaba.goldencloth.config.ProductConfig;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Random;

public final class PriceCalculator {
    private PriceCalculator() {
    }

    public static int calculateDailyPrice(ProductConfig product, LocalDate date) {
        return calculateDailyPrice(
                product.getBasePrice(),
                product.getRandomness(),
                date,
                product.getName(),
                product.getMaterial()
        );
    }

    public static int calculateDailyPrice(
            int basePrice,
            double randomness,
            LocalDate date,
            String name,
            String material
    ) {
        if (randomness <= 0.0) {
            return basePrice;
        }

        long seed = Objects.hash(
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                basePrice,
                name,
                material
        );
        Random random = new Random(seed);
        double delta = (random.nextDouble() * 2.0 - 1.0) * randomness;
        double adjusted = basePrice * (1.0 + delta);
        return Math.max(0, (int) Math.round(adjusted));
    }
}
