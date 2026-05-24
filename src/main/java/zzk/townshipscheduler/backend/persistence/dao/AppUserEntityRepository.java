package zzk.townshipscheduler.backend.persistence.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.AccountEntity;

public interface AppUserEntityRepository extends JpaRepository<AccountEntity, Long> {

    boolean existsAppUserEntitiesByUsername(String username);

    AccountEntity findByUsername(String username);

    void deleteByUsername(String username);

}
