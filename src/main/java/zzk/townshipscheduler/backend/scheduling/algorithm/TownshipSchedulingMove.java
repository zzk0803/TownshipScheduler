//package zzk.townshipscheduler.backend.scheduling.algorithm;
//
//import ai.timefold.solver.core.api.domain.common.Lookup;
//import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
//import ai.timefold.solver.core.preview.api.move.Move;
//import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
//import org.jspecify.annotations.Nullable;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingDateTimeSlot;
//import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
//import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
//
//import java.util.Objects;
//import java.util.SequencedCollection;
//
//public class TownshipSchedulingMove
//        implements Move<TownshipSchedulingProblem> {
//
//    private final PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> dataTimeVariable;
//
//    private final SchedulingProducingArrangement arrangement;
//
//    private final SchedulingDateTimeSlot dateTimeSlot;
//
//    public TownshipSchedulingMove(
//            PlanningVariableMetaModel<TownshipSchedulingProblem, SchedulingProducingArrangement, SchedulingDateTimeSlot> dataTimeVariable,
//            SchedulingProducingArrangement arrangement,
//            SchedulingDateTimeSlot dateTimeSlot
//    ) {
//        this.dataTimeVariable = Objects.requireNonNull(dataTimeVariable);
//        this.arrangement = Objects.requireNonNull(arrangement);
//        this.dateTimeSlot = dateTimeSlot;
//    }
//
//    @Override
//    public void execute(MutableSolutionView<TownshipSchedulingProblem> solutionView) {
//        solutionView.changeVariable(dataTimeVariable, arrangement, dateTimeSlot);
//    }
//
//    @Override
//    public TownshipSchedulingMove rebase(Lookup lookup) {
//        return new TownshipSchedulingMove(
//                dataTimeVariable,
//                lookup.lookUpWorkingObject(arrangement),
//                lookup.lookUpWorkingObject(dateTimeSlot)
//        );
//    }
//
//    @Override
//    public SequencedCollection<Object> getPlanningEntities() {
//        return Move.super.getPlanningEntities();
//    }
//
//    @Override
//    public SequencedCollection<@Nullable Object> getPlanningValues() {
//        return Move.super.getPlanningValues();
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(dataTimeVariable, arrangement, dateTimeSlot);
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        return o instanceof TownshipSchedulingMove other
//               && Objects.equals(dataTimeVariable, other.dataTimeVariable)
//               && Objects.equals(arrangement, other.arrangement)
//               && Objects.equals(dateTimeSlot, other.dateTimeSlot);
//    }
//
//    @Override
//    public String toString() {
//        return arrangement + " :=  " + dateTimeSlot;
//    }
//
//}
