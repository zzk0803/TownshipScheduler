package zzk.townshipscheduler.backend.scheduling;

import lombok.Builder;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.*;

import java.util.Set;

@Value
@Builder
public class TownshipSchedulingRequest {

    Set<ProductEntity> productEntities;
//    Set<ProductEntityDtoForScheduling> productEntities;

    Set<FieldFactoryInfoEntity> fieldFactoryInfoEntities;
//    Set<FieldFactoryInfoEntityDto> fieldFactoryInfoEntities;

    Set<OrderEntity> playerEntityOrderEntities;
//    Set<OrderEntityDto> playerEntityOrderEntities;

    Set<FieldFactoryEntity> playerEntityFieldFactoryEntities;
//    Set<FieldFactoryEntityDto> playerEntityFieldFactoryEntities;

    WarehouseEntity playerEntityWarehouseEntity;
//    WarehouseEntityDto playerEntityWarehouseEntity;

}
