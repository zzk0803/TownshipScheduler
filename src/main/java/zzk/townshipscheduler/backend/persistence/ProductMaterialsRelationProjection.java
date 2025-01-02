package zzk.townshipscheduler.backend.persistence;

/**
 * Projection for {@link ProductMaterialsRelation}
 */
public interface ProductMaterialsRelationProjection {

    Long getId();

    Integer getAmount();

    ProductEntityProjectionJustId getMaterial();

}
