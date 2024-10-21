package zzk.townshipscheduler.backend.scheduling.mapping;

import org.mapstruct.*;
import zzk.townshipscheduler.backend.persistence.Goods;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingGoods;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface GoodsMapper {

    Goods toEntity(SchedulingGoods schedulingGoods);

    SchedulingGoods toDto(Goods goods);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    Goods partialUpdate(
            SchedulingGoods schedulingGoods,
            @MappingTarget Goods goods
    );

}
