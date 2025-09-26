package zzk.townshipscheduler.backend.dao;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProductEntityRepository
        extends JpaRepository<ProductEntity, Long> {

    @EntityGraph(value = "products.g.full")
    <T> Set<T> findBy(Class<T> projectionClass, Sort sort);

    @EntityGraph(value = "products.g.full")
    @Query("select p from ProductEntity p where p.id=:id")
    Optional<ProductEntity> queryById(Long id);

    Optional<ProductEntity> findByName(String name);

    @Query("from ProductEntity as pe join  pe.fieldFactoryInfo  as ffe  select distinct pe.fieldFactoryInfo")
    List<FieldFactoryInfoEntity> queryFieldFactory();

//    @Query(
//            """
//                select p from ProductEntity as p
//                            left join fetch p.manufactureInfoEntities as pmi
//                                        left join fetch pmi.productMaterialsRelations as pmr
//                                                    left join fetch pmr.material
//                where p.level<=:level
//            """)
    @EntityGraph(
            attributePaths = {
                    "fieldFactoryInfo",
                    "manufactureInfoEntities",
                    "manufactureInfoEntities.productMaterialsRelations",
                    "manufactureInfoEntities.productMaterialsRelations.material"
            },
            type = EntityGraph.EntityGraphType.LOAD
    )
    @Query("select p from ProductEntity p where p.level<=:level")
    Set<ProductEntity> queryForPrepareScheduling(Integer level);

    @EntityGraph(
            attributePaths = {
                    "crawledAsImage.imageBytes"
            }
    )
    @Query("select pe.crawledAsImage.imageBytes from ProductEntity as pe where pe.id=:id")
    Optional<byte[]> queryProductImageById(Serializable id);

    @EntityGraph(
            attributePaths = {
                    "crawledAsImage.imageBytes"
            }
    )
    @Query("select pe.crawledAsImage.imageBytes from ProductEntity as pe where pe.name=:name")
    Optional<byte[]> queryProductImageByName(String name);
}
