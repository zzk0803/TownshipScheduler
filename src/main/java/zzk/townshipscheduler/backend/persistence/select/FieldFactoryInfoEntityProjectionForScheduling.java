package zzk.townshipscheduler.backend.persistence.select;

import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.util.Set;

/**
 * Projection for {@link FieldFactoryInfoEntity}
 */
public interface FieldFactoryInfoEntityProjectionForScheduling {

    Long getId();

    String getCategory();

    boolean isBoolCategoryField();

    Integer getLevel();

    ProducingStructureType getProducingType();

    Integer getDefaultInstanceAmount();

    Integer getDefaultProducingCapacity();

    Integer getDefaultReapWindowCapacity();

    Integer getMaxProducingCapacity();

    Integer getMaxReapWindowCapacity();

    Integer getMaxInstanceAmount();

    Set<ProductEntityProjectionJustId> getPortfolioGoods();

}
