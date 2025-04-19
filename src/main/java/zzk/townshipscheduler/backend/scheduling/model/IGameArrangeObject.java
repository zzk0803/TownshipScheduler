package zzk.townshipscheduler.backend.scheduling.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public sealed interface IGameArrangeObject
        permits SchedulingOrder, SchedulingProduct {

    Long longIdentity();

    String readable();

//    List<SchedulingPlayerWarehouseAction> calcWarehouseActions();
//
//    List<SchedulingPlayerWarehouseAction> calcWarehouseActions(IGameActionObject targetObject);

    List<? extends SchedulingProducingArrangement>  calcFactoryActions();

    List<? extends SchedulingProducingArrangement> calcFactoryActions(IGameArrangeObject targetObject);

    Set<SchedulingProducingExecutionMode> getExecutionModeSet();

    Optional<LocalDateTime> optionalDeadline();

}
