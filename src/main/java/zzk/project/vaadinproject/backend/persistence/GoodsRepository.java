package zzk.project.vaadinproject.backend.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GoodsRepository
        extends JpaRepository<Goods, Long> {

    Optional<Goods> findByName(String name);

    @EntityGraph(attributePaths = "imageBytes", type = EntityGraph.EntityGraphType.LOAD)
    <T> List<T> findBy(Class<T> projectionClass);

    @EntityGraph(attributePaths = "imageBytes", type = EntityGraph.EntityGraphType.LOAD)
    <T> List<T> findBy(Class<T> projectionClass, Sort sort);

    @Query(
            """
                        from Goods as g
                        select distinct g.category as category
                    """
    )
    Set<String> queryCategories();

}
