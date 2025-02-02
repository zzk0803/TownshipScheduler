package zzk.townshipscheduler.backend.persistence.select;

import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.util.Set;

/**
 * Projection for {@link ProductEntity}
 */
public interface ProductEntityProjectionForScheduling {

    Long getId();

    String getName();

    String getCategory();

    Integer getLevel();

    Integer getCost();

    Integer getSellPrice();

    Integer getXp();

    Integer getDealerValue();

    Integer getHelpValue();

    Integer getGainWhenCompleted();

    FieldFactoryInfoEntityProjectionForScheduling getFieldFactoryInfo();

    Set<ProductManufactureInfoEntityProjection> getManufactureInfoEntities();

}
