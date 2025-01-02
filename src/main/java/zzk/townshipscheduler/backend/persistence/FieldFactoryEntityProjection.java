package zzk.townshipscheduler.backend.persistence;

/**
 * Projection for {@link FieldFactoryEntity}
 */
public interface FieldFactoryEntityProjection {

    Long getId();

    int getProducingLength();

    int getReapWindowSize();

}
