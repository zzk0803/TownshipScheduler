package zzk.townshipscheduler.backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;

import java.util.Optional;

public interface WikiCrawledEntityRepository
        extends JpaRepository<WikiCrawledEntity, Long> {

    boolean existsByHtml(String html);

    boolean existsByText(String text);

    WikiCrawledEntity findByText(String text);

    @Query(
            """
            from WikiCrawledEntity as tc
            where tc.type=zzk.townshipscheduler.backend.persistence.WikiCrawledEntity.Type.HTML 
            order by tc.createdDateTime 
            limit 1
            """
    )
    Optional<WikiCrawledEntity> orderByCreatedDateTimeDescLimit1();

    @Query(
            """
            select tc.id from WikiCrawledEntity as tc
            where tc.type=zzk.townshipscheduler.backend.persistence.WikiCrawledEntity.Type.IMAGE
            and tc.html=:html
            """
    )
    Long queryIdBearImageByHtml(String html);

    @Query(
            """
             from WikiCrawledEntity as tc
            where tc.type=zzk.townshipscheduler.backend.persistence.WikiCrawledEntity.Type.IMAGE
            and tc.html=:html
            """
    )
    WikiCrawledEntity queryEntityBearImageByHtml(String html);

    @Query(
            """
            select tc.imageBytes from WikiCrawledEntity as tc
            where tc.type=zzk.townshipscheduler.backend.persistence.WikiCrawledEntity.Type.IMAGE
            and tc.html=:html
            """
    )
    byte[] queryImageBytesByHtml(String html);

}
