package zzk.townshipscheduler.backend.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;

import java.util.List;

public interface FieldFactoryEntityRepository extends JpaRepository<FieldFactoryEntity, Long> {

    @EntityGraph(attributePaths = {"fieldFactoryInfoEntity", "playerEntity"}, type = EntityGraph.EntityGraphType.LOAD)
    List<FieldFactoryEntity> findFieldFactoryEntityByPlayerEntity(PlayerEntity player);

    int countByPlayerEntityAndFieldFactoryInfoEntity(
            PlayerEntity playerEntity,
            FieldFactoryInfoEntity fieldFactoryInfoEntity
    );

}
