package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.WikiCrawledParsedCoordCellEntity;

public interface WikiCrawledParsedCoordCellEntityRepository
        extends JpaRepository<WikiCrawledParsedCoordCellEntity, Long> {

}
