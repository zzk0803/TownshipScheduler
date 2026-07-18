package zzk.townshipscheduler.backend.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;

import java.util.Collection;

public interface ProductMaterialsRelationRepository extends JpaRepository<ProductMaterialsRelation, Long> {

}
