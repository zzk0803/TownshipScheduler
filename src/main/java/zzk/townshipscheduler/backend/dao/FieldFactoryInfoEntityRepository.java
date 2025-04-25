package zzk.townshipscheduler.backend.dao;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FieldFactoryInfoEntityRepository extends JpaRepository<FieldFactoryInfoEntity, Long> {

    Optional<FieldFactoryInfoEntity> findByCategory(String category);

    <T> Set<T> findBy(Class<T> projectionClass);

    @EntityGraph(attributePaths = {"portfolioGoods"}, type = EntityGraph.EntityGraphType.LOAD)
    <T> Set<T> findBy(Class<T> projectionClass, Sort sort);

    @Query("select f from FieldFactoryInfoEntity f where f.level <= ?1  and f.boolCategoryField=false order by f.level")
    List<FieldFactoryInfoEntity> queryFactoryInfoByLevelLessThanOrEqual(Integer level);

    @EntityGraph(attributePaths = {"portfolioGoods.fieldFactoryInfo"})
    @Query("select f from FieldFactoryInfoEntity f")
    Set<FieldFactoryInfoEntity> queryForPrepareScheduling();

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
    Set<FieldFactoryInfoEntity> queryForFactoryProductSelection(Sort sort);

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
