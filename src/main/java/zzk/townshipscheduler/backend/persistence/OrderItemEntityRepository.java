package zzk.townshipscheduler.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemEntityRepository
        extends JpaRepository<OrderItemEntity, Long> {

}
