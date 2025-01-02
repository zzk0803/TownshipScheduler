//package zzk.townshipscheduler.backend.dao;
//
//import org.springframework.data.jpa.repository.EntityGraph;
//import org.springframework.data.jpa.repository.JpaRepository;
//import zzk.townshipscheduler.backend.persistence.WarehouseEntity;
//import zzk.townshipscheduler.backend.persistence.WarehouseRecordEntity;
//
//import java.util.Set;
//
//public interface WarehouseRecordEntityRepository extends JpaRepository<WarehouseRecordEntity, Long> {
//
//    @EntityGraph(attributePaths = {"productEntity"})
//    <T> Set<T> findWarehouseEntityByWarehouseEntity(Class<T> projectionClass, WarehouseEntity warehouseEntity);
//
//}
