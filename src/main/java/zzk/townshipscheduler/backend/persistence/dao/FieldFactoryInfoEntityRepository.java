package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;

import java.util.List;
import java.util.Optional;

public interface FieldFactoryInfoEntityRepository extends JpaRepository<FieldFactoryInfoEntity, Long> {

    Optional<FieldFactoryInfoEntity> findByCategory(String category);

    <T> List<T> findBy(Class<T> projectionClass);

    <T> List<T> findBy(Class<T> projectionClass, Sort sort);

    @Query("select f from FieldFactoryInfoEntity f where f.level <= ?1 order by f.level")
    List<FieldFactoryInfoEntity> queryFactoryInfoByLevelLessThanOrEqual(Integer level);

    /*
    SELECT fffi.*
    FROM FieldFactoryInfoEntity fffi
    WHERE fffi.level <= (SELECT level FROM PlayerEntity WHERE id = :playerId)
    AND (
        SELECT COUNT(*)
        FROM FieldFactoryEntity ffe
        WHERE ffe.fieldFactoryInfoEntity_id = fffi.id
        AND ffe.playerEntity_id = :playerId
    ) < fffi.maxInstanceAmount;
     */

}
