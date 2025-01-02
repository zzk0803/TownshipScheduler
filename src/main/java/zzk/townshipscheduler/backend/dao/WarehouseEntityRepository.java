package zzk.townshipscheduler.backend.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.persistence.WarehouseEntity;

public interface WarehouseEntityRepository extends JpaRepository<WarehouseEntity, Long> {

    @EntityGraph(value = "warehouse.product-amount-map", type = EntityGraph.EntityGraphType.LOAD)
    WarehouseEntity findWarehouseEntityByPlayerEntity(PlayerEntity player);

}
