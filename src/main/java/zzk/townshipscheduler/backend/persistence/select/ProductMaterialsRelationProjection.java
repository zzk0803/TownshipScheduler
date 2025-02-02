package zzk.townshipscheduler.backend.persistence.select;

import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;

/**
 * Projection for {@link ProductMaterialsRelation}
 */
public interface ProductMaterialsRelationProjection {

    Long getId();

    Integer getAmount();

    ProductEntityProjectionJustId getMaterial();

}
