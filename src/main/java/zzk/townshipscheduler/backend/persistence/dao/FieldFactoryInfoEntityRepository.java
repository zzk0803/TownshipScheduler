package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FieldFactoryInfoEntityRepository
        extends JpaRepository<FieldFactoryInfoEntity, Long> {

    @EntityGraph("fieldFactoryInfo.g.full")
    Optional<FieldFactoryInfoEntity> findByCategory(String category);

    <T> Set<T> findBy(Class<T> projectionClass);

    List<FieldFactoryInfoEntity> findFieldFactoryInfoEntitiesByLevelLessThanEqual(Integer level);

    List<FieldFactoryInfoEntity> findFieldFactoryInfoEntitiesByLevelBetween(Integer levelAfter, Integer levelBefore);

    @EntityGraph("fieldFactoryInfo.g.full")
    @Query("select ffie from FieldFactoryInfoEntity ffie")
    Set<FieldFactoryInfoEntity> queryForFactoryProductSelection(Sort sort);

    @Query("select f from FieldFactoryInfoEntity f join fetch f.productManufactureInfoSet as fpg where f.level<=:level and fpg.productEntity.level<=:level ")
    @EntityGraph("fieldFactoryInfo.g.full")
    Set<FieldFactoryInfoEntity> queryForPrepareScheduling(@Param("level") Integer level);

    @EntityGraph("fieldFactoryInfo.g.full")
    @Query("select ffie from FieldFactoryInfoEntity ffie join fetch ffie.productManufactureInfoSet as ffiep where ffie.level<=:level and ffiep.productEntity.level<=:level")
    Set<FieldFactoryInfoEntity> queryForFactoryProductSelection(Integer level, Sort sort);

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
