package zzk.townshipscheduler.backend.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;

public interface TownshipProblemEntityRepository extends JpaRepository<TownshipProblemEntity, String> {

    @EntityGraph("problem.g.full")
    <T> Optional<T> findByUuid(
            String uuid,
            Class<T> projectionClass
    );

    @Query(
            """
                 select tpe from TownshipProblemEntity as tpe
            """
    )
    @EntityGraph(attributePaths = {"problemSerialized"}, type = EntityGraph.EntityGraphType.LOAD)
    <T> Collection<T> queryAll(Class<T> projectionClass);

}
