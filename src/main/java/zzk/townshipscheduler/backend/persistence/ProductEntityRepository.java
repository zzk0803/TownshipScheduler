package zzk.townshipscheduler.backend.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface ProductEntityRepository
        extends JpaRepository<ProductEntity, Long> {

    @EntityGraph(value = "products.g.full")
    <T> Set<T> findBy(Class<T> projectionClass, Sort sort);

    @EntityGraph(value = "products.g.full")
    Optional<ProductEntity> findByName(String name);

    @Query("select p from ProductEntity p where p.name = ?1")
    Optional<ProductEntity> querySimpleByName(String name);

    @EntityGraph(value = "products.g.full")
    @Query("select p from ProductEntity p where p.name = ?1")
    Optional<ProductEntity> queryFullByName(String name);

    @EntityGraph("products.g.full")
    @Query("select p from ProductEntity p where p.level<=:level")
    Set<ProductEntity> queryForPrepareScheduling(Integer level);

    @Query("from ProductEntity p")
    <T> Set<T> queryForRawProductHierarchyGraphBuilding(Class<T> projectionClass, Sort sort);

    @EntityGraph(
            attributePaths = {
                    "crawledAsImage.imageBytes"
            }
    )
    @Query("select pe.crawledAsImage.imageBytes from ProductEntity as pe where pe.name=:name")
    Optional<byte[]> queryProductImageByName(String name);

    @EntityGraph(
            attributePaths = {
                    "crawledAsImage.imageBytes"
            }
    )
    @Query("select pe.crawledAsImage.imageBytes from ProductEntity as pe where pe.id=:id")
    Optional<byte[]> queryProductImageById(Long id);



}
