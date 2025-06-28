package zzk.townshipscheduler.backend.scheduling;

import lombok.Builder;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.scheduling.model.DateTimeSlotSize;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;

@Value
@Builder
public class TownshipSchedulingRequest {

    Collection<ProductEntity> productEntities;
//    Set<ProductEntityDtoForScheduling> productEntities;

    Collection<FieldFactoryInfoEntity> fieldFactoryInfoEntities;
//    Set<FieldFactoryInfoEntityDto> fieldFactoryInfoEntities;

    Collection<OrderEntity> playerEntityOrderEntities;
//    Set<OrderEntityDto> playerEntityOrderEntities;

    Collection<FieldFactoryEntity> playerEntityFieldFactoryEntities;
//    Set<FieldFactoryEntityDto> playerEntityFieldFactoryEntities;

    WarehouseEntity playerEntityWarehouseEntity;
//    WarehouseEntityDto playerEntityWarehouseEntity;

    DateTimeSlotSize dateTimeSlotSize;

    LocalDateTime workCalendarStart;

    LocalDateTime workCalendarEnd;

    LocalTime sleepStartPickerValue;

    LocalTime sleepEndPickerValue;

}
