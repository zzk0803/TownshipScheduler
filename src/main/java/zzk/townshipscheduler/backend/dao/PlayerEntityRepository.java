package zzk.townshipscheduler.backend.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zzk.townshipscheduler.backend.persistence.AccountEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;

import java.util.List;
import java.util.Optional;

public interface PlayerEntityRepository extends JpaRepository<PlayerEntity, Long> {


    @EntityGraph(value = "player.full")
    <T> List<T> findBy(Class<T> projectionClass);

    @EntityGraph(value = "player.full")
    Optional<PlayerEntity> findPlayerEntitiesByAccount(AccountEntity appUser);

    @EntityGraph(value = "player.full")
    Optional<PlayerEntity> findPlayerById(Long playerId);

    @EntityGraph(
            attributePaths = {
                    "warehouseEntity.productAmountMap",
                    "fieldFactoryEntities",
                    "fieldFactoryEntities.fieldFactoryInfoEntity",
                    "orderEntities.productAmountMap"
            }
    )
    @Query("select p from PlayerEntity p where p.id=:playerId")
    Optional<PlayerEntity> queryForPrepareScheduling(@Param("playerId") Long playerId);

}
