package zzk.townshipscheduler.backend.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldFactoryEntityRepository extends JpaRepository<FieldFactoryEntity, Long> {

    @EntityGraph(value = "fieldFactoryEntity.g.full", type = EntityGraph.EntityGraphType.LOAD)
    List<FieldFactoryEntity> findFieldFactoryEntityByPlayerEntity(PlayerEntity player);

    int countByPlayerEntityAndFieldFactoryInfoEntity(
            PlayerEntity playerEntity,
            FieldFactoryInfoEntity fieldFactoryInfoEntity
    );

}
