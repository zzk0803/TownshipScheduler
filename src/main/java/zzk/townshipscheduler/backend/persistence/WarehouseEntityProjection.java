package zzk.townshipscheduler.backend.persistence;

import java.util.Map;

/**
 * Projection for {@link WarehouseEntity}
 */
public interface WarehouseEntityProjection {

    Long getId();

    Map<ProductEntity, Integer> getProductAmountMap();

}
