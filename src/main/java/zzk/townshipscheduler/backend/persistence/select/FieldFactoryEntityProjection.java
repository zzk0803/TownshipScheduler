package zzk.townshipscheduler.backend.persistence.select;

import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;

/**
 * Projection for {@link FieldFactoryEntity}
 */
public interface FieldFactoryEntityProjection {

    Long getId();

    int getProducingLength();

    int getReapWindowSize();

}
