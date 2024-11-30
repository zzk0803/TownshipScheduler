package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.OrderEntity;

import java.util.List;

public interface OrderEntityRepository
        extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = "productAmountPairs", type = EntityGraph.EntityGraphType.LOAD)
    <T> List<T> findBy(Class<T> projectionClazz);

}
