package zzk.townshipscheduler.backend.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRepository
        extends JpaRepository<Bill, Long> {

    @EntityGraph(attributePaths = "productAmountPairs", type = EntityGraph.EntityGraphType.LOAD)
    <T> List<T> findBy(Class<T> projectionClazz);

}
