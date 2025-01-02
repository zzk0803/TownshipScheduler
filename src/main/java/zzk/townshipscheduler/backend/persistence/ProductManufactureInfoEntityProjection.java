package zzk.townshipscheduler.backend.persistence;

import java.time.Duration;
import java.util.Set;

/**
 * Projection for {@link ProductManufactureInfoEntity}
 */
public interface ProductManufactureInfoEntityProjection {

    Long getId();

    Duration getProducingDuration();

    Set<ProductMaterialsRelationProjection> getProductMaterialsRelations();

}
