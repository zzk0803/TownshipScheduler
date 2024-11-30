package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.util.List;
import java.util.Optional;

public interface ProductEntityRepository
        extends JpaRepository<ProductEntity, Long> {

    @EntityGraph(value = "products.g.full",type = EntityGraph.EntityGraphType.LOAD)
    <T> List<T> findBy(Class<T> projectionClass, Sort sort);

    Optional<ProductEntity> findByName(String name);

    List<ProductEntity> findByCategory(String category);

    @Query("from ProductEntity as g  select distinct g.category as category")
    List<String> queryCategories();

    @Query("from ProductEntity as pe join  pe.fieldFactoryInfo  as ffe  select distinct pe.fieldFactoryInfo")
    List<FieldFactoryInfoEntity> queryFieldFactory();

}
