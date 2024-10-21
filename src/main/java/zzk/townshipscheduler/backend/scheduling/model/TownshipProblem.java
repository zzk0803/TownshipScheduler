package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@PlanningSolution
public class TownshipProblem {

    private Long id;

    private SchedulingPlayer player;

    private Set<SchedulingGoods> goodsSet = new HashSet<>();

    @ProblemFactCollectionProperty
    private Set<SchedulingBill> billList = new HashSet<>();

    @PlanningEntityCollectionProperty
    private Set<SchedulingPlantSlot> plantSlotList = new HashSet<>();

    @PlanningEntityCollectionProperty
    private Set<SchedulingGameMove> gameMoveList = new HashSet<>();

    @PlanningScore
    private HardMediumSoftLongScore score;

    private SolverStatus solverStatus;


}
