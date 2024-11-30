package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.AccountEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;

import java.util.List;
import java.util.Optional;

public interface PlayerEntityRepository extends JpaRepository<PlayerEntity, Long> {


    @EntityGraph(
            attributePaths = {
                    "warehouseEntity",
                    "warehouseEntity.itemAmountMap",
                    "fieldFactoryEntities",
                    "account",
            }
    )
    <T> List<T> findBy(Class<T> projectionClass);

    @EntityGraph(
            attributePaths = {
                    "warehouseEntity",
                    "warehouseEntity.itemAmountMap",
                    "fieldFactoryEntities",
                    "account",
            }
    )
    Optional<PlayerEntity> findPlayerEntitiesByAccount(AccountEntity appUser);


}
