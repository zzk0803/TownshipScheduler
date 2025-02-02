package zzk.townshipscheduler.backend.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;

import java.util.List;

public interface OrderEntityRepository
        extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(value = "order.project-amount-map",type = EntityGraph.EntityGraphType.LOAD)
    @Query("select oe from OrderEntity as oe where oe.playerEntity=:player")
    List<OrderEntity> queryForOrderListView(@Param("player") PlayerEntity player);


}
