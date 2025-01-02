package zzk.townshipscheduler.backend.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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

//    @EntityGraph(
//            value = "player.full"
//    )
    @EntityGraph(value = "player.full")
    <T> Optional<T> findPlayerEntitiesByAccount(AccountEntity appUser, Class<T> projectionClass);

    @EntityGraph(value = "player.full")
    <T> Optional<T> findPlayerById(Long playerId, Class<T> projectionClass);

}
