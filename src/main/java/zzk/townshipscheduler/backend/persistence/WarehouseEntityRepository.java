package zzk.townshipscheduler.backend.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseEntityRepository extends JpaRepository<WarehouseEntity, Long> {

    @EntityGraph(value = "warehouse.items", type = EntityGraph.EntityGraphType.LOAD)
    WarehouseEntity findWarehouseEntityByPlayerEntity(PlayerEntity player);

}
