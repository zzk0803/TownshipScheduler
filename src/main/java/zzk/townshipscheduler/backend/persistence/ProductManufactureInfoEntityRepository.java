package zzk.townshipscheduler.backend.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

public interface ProductManufactureInfoEntityRepository
        extends JpaRepository<ProductManufactureInfoEntity, Long> {

    @EntityGraph(
            attributePaths = {
                    "productEntity.crawledAsImage.imageBytes",
                    "productMaterialsRelations.material.crawledAsImage.imageBytes",
                    "productMaterialsRelations.material.manufactureInfoEntities"
            }
    )
    @Query("select p from ProductManufactureInfoEntity p where p.productEntity = :productEntity")
    Set<ProductManufactureInfoEntity> queryMaterials(@Param("productEntity") ProductEntity productEntity);

    @EntityGraph(
            attributePaths = {
                    "productEntity.crawledAsImage.imageBytes",
                    "productMaterialsRelations.material.crawledAsImage.imageBytes",
                    "productMaterialsRelations.material.manufactureInfoEntities"
            }
    )
    @Query(
            """
            select pmi from ProductManufactureInfoEntity as pmi
                        inner join  pmi.productMaterialsRelations as pmr
                                    inner join  pmr.material as pm
            where pm = :productEntity
            """
    )
    Set<ProductManufactureInfoEntity> queryComposite(@Param("productEntity") ProductEntity productEntity);


}
