package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;

import java.util.List;
import java.util.Set;

public interface ProductManufactureInfoEntityRepository extends JpaRepository<ProductManufactureInfoEntity, Long> {

    @EntityGraph("product-manufacture-info.g.full")
    @Query(
            """
            select p from ProductManufactureInfoEntity p inner join p.productMaterialsRelations as pmr
            where pmr.material = :material
            """
    )
    Set<ProductManufactureInfoEntity> queryProductManufactureInfoByMaterial(@Param("material") ProductEntity material);

}
