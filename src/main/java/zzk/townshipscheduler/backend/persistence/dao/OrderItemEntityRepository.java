package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.OrderItemEntity;

public interface OrderItemEntityRepository
        extends JpaRepository<OrderItemEntity, Long> {

}
