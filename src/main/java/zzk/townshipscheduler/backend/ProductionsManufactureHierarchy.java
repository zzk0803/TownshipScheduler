package zzk.townshipscheduler.backend;

import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.util.Map;

public class ProductionsManufactureHierarchy {

    private final Map<ProductEntity, Integer> itemsMap;

    private int level = 0;

    private ProductionsManufactureHierarchy deeper;

    public ProductionsManufactureHierarchy(Map<ProductEntity, Integer> itemsMap) {
        this.itemsMap = itemsMap;
    }

    public ProductionsManufactureHierarchy setupDeeper(Map<ProductEntity, Integer> itemsMap) {
        ProductionsManufactureHierarchy productionsManufactureHierarchy = new ProductionsManufactureHierarchy(itemsMap);
        return this.deeper = productionsManufactureHierarchy;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

}
