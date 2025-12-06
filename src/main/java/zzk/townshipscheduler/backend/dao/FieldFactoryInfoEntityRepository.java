package zzk.townshipscheduler.backend.dao;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FieldFactoryInfoEntityRepository extends JpaRepository<FieldFactoryInfoEntity, Long> {

    Optional<FieldFactoryInfoEntity> findByCategory(String category);

    <T> Set<T> findBy(Class<T> projectionClass);

    List<FieldFactoryInfoEntity> findFieldFactoryInfoEntitiesByLevelLessThan(Integer level);

    List<FieldFactoryInfoEntity> findFieldFactoryInfoEntitiesByLevelBetween(Integer levelAfter, Integer levelBefore);

    @Query("select f from FieldFactoryInfoEntity f join fetch f.portfolioGoods as fpg where f.level<=:level and fpg.level<=:level ")
    Set<FieldFactoryInfoEntity> queryForPrepareScheduling(@Param("level") Integer level);

    @EntityGraph(
            attributePaths = {
                    "portfolioGoods",
                    "portfolioGoods.crawledAsImage.imageBytes"
            }
    )
    @Query("select ffie from FieldFactoryInfoEntity ffie")
    Set<FieldFactoryInfoEntity> queryForFactoryProductSelection();

    @EntityGraph(
            attributePaths = {
                    "portfolioGoods",
                    "portfolioGoods.crawledAsImage.imageBytes"
            }
    )
    @Query("select ffie from FieldFactoryInfoEntity ffie")
    @Cacheable(cacheNames = {"cache::factoryProducts"})
    Set<FieldFactoryInfoEntity> queryForFactoryProductSelection(Sort sort);

    @EntityGraph(
            attributePaths = {
                    "portfolioGoods",
                    "portfolioGoods.manufactureInfoEntities.productMaterialsRelations",
                    "portfolioGoods.crawledAsImage.imageBytes"
            }
    )
    @Query("select ffie from FieldFactoryInfoEntity ffie join fetch ffie.portfolioGoods as ffieg where ffie.level<=:level and ffieg.level<=:level")
    @Cacheable(cacheNames = {"cache::factoryProducts:level"}, key = "#level")
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
