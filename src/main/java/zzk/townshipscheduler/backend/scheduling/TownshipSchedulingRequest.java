package zzk.townshipscheduler.backend.scheduling;

import lombok.Builder;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.*;

import java.util.Set;

@Value
@Builder
public class TownshipSchedulingRequest {

    Set<ProductEntityProjectionForScheduling> productEntities;
//    Set<ProductEntityDtoForScheduling> productEntities;

    Set<FieldFactoryInfoEntityProjectionForScheduling> fieldFactoryInfoEntities;
//    Set<FieldFactoryInfoEntityDto> fieldFactoryInfoEntities;

    Set<OrderEntityProjection> playerEntityOrderEntities;
//    Set<OrderEntityDto> playerEntityOrderEntities;

    Set<FieldFactoryEntityProjection> playerEntityFieldFactoryEntities;
//    Set<FieldFactoryEntityDto> playerEntityFieldFactoryEntities;

    WarehouseEntityProjection playerEntityWarehouseEntity;
//    WarehouseEntityDto playerEntityWarehouseEntity;

}
