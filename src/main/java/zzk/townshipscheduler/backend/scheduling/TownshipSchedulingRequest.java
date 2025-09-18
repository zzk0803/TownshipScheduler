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

    Collection<FieldFactoryInfoEntity> fieldFactoryInfoEntities;

    Collection<OrderEntity> playerEntityOrderEntities;

    Collection<FieldFactoryEntity> playerEntityFieldFactoryEntities;

    WarehouseEntity playerEntityWarehouseEntity;

    DateTimeSlotSize dateTimeSlotSize;

    LocalDateTime workCalendarStart;

    LocalDateTime workCalendarEnd;

    LocalTime sleepStartPickerValue;

    LocalTime sleepEndPickerValue;

}
