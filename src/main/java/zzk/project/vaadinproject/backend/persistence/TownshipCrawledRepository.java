package zzk.project.vaadinproject.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TownshipCrawledRepository
        extends JpaRepository<TownshipCrawled, Long> {

    boolean existsByHtml(String html);

    boolean existsByText(String text);

    TownshipCrawled findByText(String text);

    @Query("from TownshipCrawled as tc where tc.type=zzk.project.vaadinproject.backend.persistence.TownshipCrawled.Type.HTML order by tc.createdDateTime limit 1")
    Optional<TownshipCrawled> orderByCreatedDateTimeDescLimit1();

    @Query("select tc.id from TownshipCrawled as tc  where tc.type=zzk.project.vaadinproject.backend.persistence.TownshipCrawled.Type.IMAGE and tc.html=:html")
    Long queryIdBearImageByHtml(String html);

    @Query("select tc.imageBytes from TownshipCrawled as tc where tc.type=zzk.project.vaadinproject.backend.persistence.TownshipCrawled.Type.IMAGE and tc.html=:html")
    byte[] queryImageBytesByHtml(String html);

}
