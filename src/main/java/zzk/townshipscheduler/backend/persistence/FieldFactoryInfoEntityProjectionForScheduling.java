package zzk.townshipscheduler.backend.persistence;

import java.util.Set;

/**
 * Projection for {@link FieldFactoryInfoEntity}
 */
public interface FieldFactoryInfoEntityProjectionForScheduling {

    Long getId();

    String getCategory();

    boolean isBoolCategoryField();

    Integer getLevel();

    FieldFactoryProducingType getProducingType();

    Integer getDefaultInstanceAmount();

    Integer getDefaultProducingCapacity();

    Integer getDefaultReapWindowCapacity();

    Integer getMaxProducingCapacity();

    Integer getMaxReapWindowCapacity();

    Integer getMaxInstanceAmount();

    Set<ProductEntityProjectionJustId> getPortfolioGoods();

}
