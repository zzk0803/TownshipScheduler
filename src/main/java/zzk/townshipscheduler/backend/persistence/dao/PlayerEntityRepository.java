package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import zzk.townshipscheduler.backend.persistence.AccountEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;

import java.util.List;
import java.util.Optional;

public interface PlayerEntityRepository
        extends JpaRepository<PlayerEntity, Long> {

    @EntityGraph(value = "player.full")
    <T> List<T> findBy(Class<T> projectionClass);

    @Transactional(readOnly = true)
    @EntityGraph(
            value = "player.full",
            type = EntityGraph.EntityGraphType.LOAD
    )
    Optional<PlayerEntity> findPlayerEntitiesByAccount(AccountEntity appUser);

    @EntityGraph(value = "player.full")
    Optional<PlayerEntity> findPlayerById(Long playerId);

    @EntityGraph(
            attributePaths = {
                    "warehouseEntity.warehouseItemEntities.product",
                    "fieldFactoryEntities",
                    "fieldFactoryEntities.fieldFactoryInfoEntity",
                    "orderEntities.orderItemEntities.productEntity"
            }
    )
    @Query("select p from PlayerEntity p where p.id=:playerId")
    @Transactional(readOnly = true)
    Optional<PlayerEntity> queryForPrepareScheduling(@Param("playerId") Long playerId);

}
