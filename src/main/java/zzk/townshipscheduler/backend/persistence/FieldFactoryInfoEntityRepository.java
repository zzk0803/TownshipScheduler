package zzk.townshipscheduler.backend.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(
            attributePaths = {
                    "productEntities",
                    "productEntities.manufactureInfoEntities.productMaterialsRelations",
                    "productEntities.crawledAsImage.imageBytes"
            }
    )
    @Query("from FieldFactoryInfoEntity as ffie")
    Set<FieldFactoryInfoEntity> queryForFactoryProductSelection(Sort sort);

    @Query("select f from FieldFactoryInfoEntity f join fetch f.productEntities as pe where f.level<=:level and pe.level<=:level ")
    @EntityGraph(
            attributePaths = {
                    "productEntities",
                    "productEntities.manufactureInfoEntities.productMaterialsRelations",
                    "productEntities.crawledAsImage.imageBytes"
            }
    )
    Set<FieldFactoryInfoEntity> queryForPrepareScheduling(@Param("level") Integer level);

    @EntityGraph("fieldFactoryInfo.g.full")
    @Query("select ffie from FieldFactoryInfoEntity ffie join fetch ffie.productEntities as ffiep where ffie.level<=:level and ffiep.level<=:level")
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
