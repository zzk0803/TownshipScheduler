package zzk.townshipscheduler.backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.TownshipArrangementEntity;

public interface TownshipArrangementEntityRepository extends JpaRepository<TownshipArrangementEntity, String> {

}
