package zzk.townshipscheduler.backend.persistence.select;

import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.WarehouseEntity;

import java.util.Map;

/**
 * Projection for {@link WarehouseEntity}
 */
public interface WarehouseEntityProjection {

    Long getId();

    Map<ProductEntity, Integer> getProductAmountMap();

}
