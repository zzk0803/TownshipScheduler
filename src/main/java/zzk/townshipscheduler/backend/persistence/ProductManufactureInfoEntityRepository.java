package zzk.townshipscheduler.backend.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
