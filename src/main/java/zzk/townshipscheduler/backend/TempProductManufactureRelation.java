package zzk.townshipscheduler.backend;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class TempProductManufactureRelation {

    private int id;

    private ProductId productId;

    private List<ProductId> composite;

    private Map<ProductId, Integer> materials;

    private Boolean atomicProduct;

    public boolean boolAtomicProduct() {
        if (atomicProduct == null) {
            atomicProduct = materials == null || materials.isEmpty();
        }
        return atomicProduct;
    }

}
