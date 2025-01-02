package zzk.townshipscheduler.backend.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import zzk.townshipscheduler.backend.persistence.AccountEntity;

public interface AppUserEntityRepository extends JpaRepository<AccountEntity, Long>, JpaSpecificationExecutor<AccountEntity> {

    boolean existsAppUserEntitiesByUsername(String username);

    AccountEntity findByUsername(String username);

    void deleteByUsername(String username);

}
