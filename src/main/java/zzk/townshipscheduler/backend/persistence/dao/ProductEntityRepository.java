package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ProductEntityRepository
        extends JpaRepository<ProductEntity, Long> {

    @EntityGraph(value = "products.g.full")
    <T> Set<T> findBy(Class<T> projectionClass, Sort sort);

    Optional<ProductEntity> findByName(String name);

    @EntityGraph("products.g.full")
    @Query("select p from ProductEntity p where p.level<=:level")
    Set<ProductEntity> queryForPrepareScheduling(Integer level);

    @Transactional(readOnly = true)
    @Query("from ProductEntity p")
    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH)
    <T> Set<T> queryForRawProductHierarchyGraphBuilding(Class<T> projectionClass, Sort sort);

    @Transactional(readOnly = true)
    @EntityGraph(
            attributePaths = {
                    "crawledAsImage.imageBytes"
            }
    )
    @Query("select pe.crawledAsImage.imageBytes from ProductEntity as pe where pe.name=:name")
    Optional<byte[]> queryProductImageByName(String name);



}
