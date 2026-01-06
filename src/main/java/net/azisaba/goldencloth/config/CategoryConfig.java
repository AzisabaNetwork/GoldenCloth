package net.azisaba.goldencloth.config;

import java.util.Collections;
import java.util.List;

public class CategoryConfig {
    private final String name;
    private final String material;
    private final List<ProductConfig> products;

    public CategoryConfig(String name, String material, List<ProductConfig> products) {
        this.name = name;
        this.material = material;
        this.products = Collections.unmodifiableList(products);
    }

    public String getName() {
        return name;
    }

    public String getMaterial() {
        return material;
    }

    public List<ProductConfig> getProducts() {
        return products;
    }
}
