package zzk.townshipscheduler.backend.scheduling.mapping;

import org.mapstruct.*;
import zzk.townshipscheduler.backend.persistence.Bill;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingBill;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface BillMapper {

    Bill toEntity(SchedulingBill billDto);

    SchedulingBill toDto(Bill bill);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Bill partialUpdate(
            SchedulingBill billDto,
            @MappingTarget Bill bill
    );

}
