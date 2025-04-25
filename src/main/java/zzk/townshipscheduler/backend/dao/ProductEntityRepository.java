package zzk.townshipscheduler.backend.dao;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

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

    @EntityGraph(
            attributePaths = {
                    "fieldFactoryInfo",
                    "manufactureInfoEntities",
                    "manufactureInfoEntities.productMaterialsRelations",
                    "manufactureInfoEntities.productMaterialsRelations.material",
                    "manufactureInfoEntities.productMaterialsRelations.productManufactureInfo"
            }
    )
    @Query("select p from ProductEntity p")
    Set<ProductEntity> queryForPrepareScheduling();

}
