package zzk.townshipscheduler.backend.scheduling.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public sealed interface SchedulingGameActionObject
        permits SchedulingOrder, SchedulingProduct {

    String readable();

    List<SchedulingPlayerWarehouseAction> calcWarehouseActions();

    List<SchedulingPlayerFactoryAction> calcFactoryActions();

    List<SchedulingPlayerFactoryAction> calcFactoryActions(SchedulingGameActionObject targetObject);

    Set<SchedulingGameActionExecutionMode> getExecutionModeSet();

    Optional<LocalDateTime> optionalDeadline();

}
