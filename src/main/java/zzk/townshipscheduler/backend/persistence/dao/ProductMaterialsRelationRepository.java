package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;

public interface ProductMaterialsRelationRepository extends JpaRepository<ProductMaterialsRelation, Long> {

}
