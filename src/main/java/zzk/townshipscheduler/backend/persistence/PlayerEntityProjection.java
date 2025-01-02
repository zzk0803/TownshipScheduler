package zzk.townshipscheduler.backend.persistence;

import java.util.Set;

/**
 * Projection for {@link PlayerEntity}
 */
public interface PlayerEntityProjection {

    Long getId();

    Integer getLevel();

    AccountEntityProjectionJustId getAccount();

    Set<FieldFactoryEntityProjection> getFieldFactoryEntities();

    Set<OrderEntityProjection> getOrderEntities();

    WarehouseEntityProjection getWarehouseEntity();

}
