package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Data
@PlanningSolution
public class TownshipSchedulingProblem {

    private UUID uuid;

    private LocalDateTime dateTime=LocalDateTime.now();

    @ProblemFactProperty
    private SchedulingGamePlayer schedulingGamePlayer;

    @ProblemFactCollectionProperty
    private List<SchedulingProduct> schedulingProductList;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> schedulingOrderList;

    @ProblemFactCollectionProperty
    private List<SchedulingFactory> schedulingFactoryList;

    @PlanningEntityCollectionProperty
    private List<SchedulingFactorySlot> schedulingFactorySlotList;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    private List<SchedulingProducing> schedulingProducingList;

    @PlanningScore
    private HardMediumSoftLongScore score;

    private SolverStatus solverStatus;

//    @ValueRangeProvider(id = "producingBeginDateTime")
//    public CountableValueRange<LocalDateTime> producingBeginDateTime() {
//        return ValueRangeFactory.createLocalDateTimeValueRange(
//                LocalDateTime.now(),
//                LocalDateTime.now().plusDays(1),
//                5,
//                ChronoUnit.MINUTES
//        );
//    }

}
