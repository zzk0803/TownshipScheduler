package zzk.townshipscheduler.backend.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;

import java.util.List;

public interface OrderEntityRepository
        extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(value = "order.project-amount-map",type = EntityGraph.EntityGraphType.LOAD)
    <T> List<T> findByPlayerEntity(PlayerEntity player, Class<T> projectionClazz);


}
