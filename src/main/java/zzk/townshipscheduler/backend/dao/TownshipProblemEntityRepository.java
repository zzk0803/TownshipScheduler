package zzk.townshipscheduler.backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.TownshipProblemEntity;

public interface TownshipProblemEntityRepository extends JpaRepository<TownshipProblemEntity, String> {

}
