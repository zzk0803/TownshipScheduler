package zzk.townshipscheduler.backend.scheduling;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.*;
import zzk.townshipscheduler.backend.scheduling.model.DateTimeSlotSize;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Value
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TownshipSchedulingRequest implements Cloneable{

    @EqualsAndHashCode.Include
    UUID requestId;

    Collection<ProductEntity> productEntities;

    Collection<FieldFactoryInfoEntity> fieldFactoryInfoEntities;

    Collection<OrderEntity> playerEntityOrderEntities;

    Collection<FieldFactoryEntity> playerEntityFieldFactoryEntities;

    WarehouseEntity playerEntityWarehouseEntity;

    DateTimeSlotSize dateTimeSlotSize;

    LocalDateTime workCalendarStart;

//    LocalDateTime workCalendarEnd;

    LocalTime sleepStartPickerValue;

    LocalTime sleepEndPickerValue;

    @Override
    protected TownshipSchedulingRequest clone() throws CloneNotSupportedException {
        TownshipSchedulingRequest townshipSchedulingRequest = (TownshipSchedulingRequest) super.clone();
        return TownshipSchedulingRequest.builder()
                .requestId(townshipSchedulingRequest.requestId)
                .productEntities(new ArrayList<>(townshipSchedulingRequest.productEntities))
                .fieldFactoryInfoEntities(new ArrayList<>(townshipSchedulingRequest.fieldFactoryInfoEntities))
                .playerEntityOrderEntities(new ArrayList<>(townshipSchedulingRequest.playerEntityOrderEntities))
                .playerEntityFieldFactoryEntities(new ArrayList<>(townshipSchedulingRequest.playerEntityFieldFactoryEntities))
                .playerEntityWarehouseEntity(townshipSchedulingRequest.playerEntityWarehouseEntity)
                .dateTimeSlotSize(townshipSchedulingRequest.dateTimeSlotSize)
                .workCalendarStart(townshipSchedulingRequest.workCalendarStart)
//                        .workCalendarEnd(workCalendarEnd)
                .sleepStartPickerValue(townshipSchedulingRequest.sleepStartPickerValue)
                .sleepEndPickerValue(townshipSchedulingRequest.sleepEndPickerValue)
                .build();
    }

}
