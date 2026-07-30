package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;

import java.util.List;

public interface ProductManufactureInfoEntityRepository extends JpaRepository<ProductManufactureInfoEntity, Long> {

}
