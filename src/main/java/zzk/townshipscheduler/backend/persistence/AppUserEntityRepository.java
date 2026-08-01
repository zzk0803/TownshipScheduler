package zzk.townshipscheduler.backend.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserEntityRepository extends JpaRepository<AccountEntity, Long> {

    boolean existsAppUserEntitiesByUsername(String username);

    AccountEntity findByUsername(String username);

    void deleteByUsername(String username);

}
