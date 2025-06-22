package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@PlanningSolution
@NoArgsConstructor
public class TownshipSchedulingProblem {

    public static final int BENDABLE_SCORE_HARD_SIZE = 2;

    public static final int BENDABLE_SCORE_SOFT_SIZE = 2;

    public static final int HARD_BROKEN_FACTORY_ABILITY = 0;

    public static final int HARD_BROKEN_PRODUCE_PREREQUISITE = 1;

    public static final int SOFT_TOLERANCE = 0;

    public static final int SOFT_BATTER = 1;

    public static final String VALUE_RANGE_FOR_FACTORIES = "valueRangeForFactories";

    public static final String VALUE_RANGE_FOR_DATE_TIME_SLOT = "valueRangeForDateTimeSlot";

    private String uuid;

    @ProblemFactCollectionProperty
    private List<SchedulingProduct> schedulingProductList;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInfo> schedulingFactoryInfoList;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> schedulingOrderList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = VALUE_RANGE_FOR_FACTORIES)
    private List<SchedulingFactoryInstance> schedulingFactoryInstanceList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = VALUE_RANGE_FOR_DATE_TIME_SLOT)
    private List<SchedulingDateTimeSlot> schedulingDateTimeSlots;

    @PlanningEntityCollectionProperty
    private List<SchedulingProducingArrangement> schedulingProducingArrangementList;

    @ProblemFactProperty
    private SchedulingWorkCalendar schedulingWorkCalendar;

    @ProblemFactProperty
    private SchedulingPlayer schedulingPlayer;

    @PlanningScore(
            bendableHardLevelsSize = BENDABLE_SCORE_HARD_SIZE,
            bendableSoftLevelsSize = BENDABLE_SCORE_SOFT_SIZE
    )
    private BendableScore score;

    private DateTimeSlotSize slotSize;

    private SolverStatus solverStatus;

    public TownshipSchedulingProblem(
            String uuid,
            List<SchedulingProduct> schedulingProductList,
            List<SchedulingFactoryInfo> schedulingFactoryInfoList,
            List<SchedulingOrder> schedulingOrderList,
            List<SchedulingFactoryInstance> schedulingFactoryInstanceList,
            List<SchedulingDateTimeSlot> schedulingDateTimeSlots,
            List<SchedulingProducingArrangement> schedulingProducingArrangementList,
            SchedulingWorkCalendar schedulingWorkCalendar,
            SchedulingPlayer schedulingPlayer,
            BendableScore score,
            DateTimeSlotSize slotSize,
            SolverStatus solverStatus
    ) {
        this.uuid = uuid;
        this.schedulingProductList = schedulingProductList;
        this.schedulingFactoryInfoList = schedulingFactoryInfoList;
        this.schedulingOrderList = schedulingOrderList;
        this.schedulingFactoryInstanceList = schedulingFactoryInstanceList;
        this.schedulingDateTimeSlots = schedulingDateTimeSlots;
        this.schedulingProducingArrangementList = schedulingProducingArrangementList;
        this.schedulingWorkCalendar = schedulingWorkCalendar;
        this.schedulingPlayer = schedulingPlayer;
        this.score = score;
        this.slotSize = slotSize;
        this.solverStatus = solverStatus;
    }

    public static TownshipSchedulingProblemBuilder builder() {
        return new TownshipSchedulingProblemBuilder();
    }

    public List<SchedulingProducingArrangement> lookupProducingArrangements(SchedulingFactoryInstance schedulingFactoryInstance) {
        return getSchedulingProducingArrangementList().stream()
                .filter(schedulingProducingArrangement -> schedulingProducingArrangement.getPlanningFactoryInstance() == schedulingFactoryInstance)
                .toList();
    }

    public Optional<SchedulingFactoryInstance> lookupFactoryInstance(FactoryReadableIdentifier factoryReadableIdentifier) {
        return getSchedulingFactoryInstanceList().stream()
                .filter(schedulingFactoryInstance -> schedulingFactoryInstance.getFactoryReadableIdentifier()
                        .equals(factoryReadableIdentifier))
                .findFirst();
    }

    public static class TownshipSchedulingProblemBuilder {

        private String uuid;

        private List<SchedulingProduct> schedulingProductList;

        private List<SchedulingFactoryInfo> schedulingFactoryInfoList;

        private List<SchedulingOrder> schedulingOrderList;

        private List<SchedulingFactoryInstance> schedulingFactoryInstanceList;

        private List<SchedulingDateTimeSlot> schedulingDateTimeSlots;

        private List<SchedulingProducingArrangement> schedulingProducingArrangementList;

        private SchedulingWorkCalendar schedulingWorkCalendar;

        private SchedulingPlayer schedulingPlayer;

        private BendableScore score;

        private DateTimeSlotSize slotSize;

        private SolverStatus solverStatus;

        TownshipSchedulingProblemBuilder() {
        }

        public TownshipSchedulingProblemBuilder uuid() {
            this.uuid = UUID.randomUUID().toString();
            return this;
        }

        public TownshipSchedulingProblemBuilder schedulingProductList(List<SchedulingProduct> schedulingProductList) {
            this.schedulingProductList = schedulingProductList;
            return this;
        }

        public TownshipSchedulingProblemBuilder schedulingFactoryInfoList(List<SchedulingFactoryInfo> schedulingFactoryInfoList) {
            this.schedulingFactoryInfoList = schedulingFactoryInfoList;
            return this;
        }

        public TownshipSchedulingProblemBuilder schedulingOrderList(List<SchedulingOrder> schedulingOrderList) {
            this.schedulingOrderList = schedulingOrderList;
            return this;
        }

        public TownshipSchedulingProblemBuilder schedulingFactoryInstanceList(List<SchedulingFactoryInstance> schedulingFactoryInstanceList) {
            this.schedulingFactoryInstanceList = schedulingFactoryInstanceList;
            return this;
        }

        public TownshipSchedulingProblemBuilder schedulingWorkCalendar(SchedulingWorkCalendar schedulingWorkCalendar) {
            this.schedulingWorkCalendar = schedulingWorkCalendar;
            return this;
        }

        public TownshipSchedulingProblemBuilder schedulingPlayer(SchedulingPlayer schedulingPlayer) {
            this.schedulingPlayer = schedulingPlayer;
            return this;
        }

        public TownshipSchedulingProblemBuilder score(BendableScore score) {
            this.score = score;
            return this;
        }

        public TownshipSchedulingProblemBuilder dateTimeSlotSize(DateTimeSlotSize slotSize) {
            this.slotSize = slotSize;
            return this;
        }

        public TownshipSchedulingProblemBuilder solverStatus(SolverStatus solverStatus) {
            this.solverStatus = solverStatus;
            return this;
        }

        public TownshipSchedulingProblem build() {
            this.setupDateTimeSlot();
            this.setupGameActions();
            this.trimUnrelatedObject();

            return new TownshipSchedulingProblem(
                    this.uuid,
                    this.schedulingProductList,
                    this.schedulingFactoryInfoList,
                    this.schedulingOrderList,
                    this.schedulingFactoryInstanceList,
                    this.schedulingDateTimeSlots,
                    this.schedulingProducingArrangementList,
                    this.schedulingWorkCalendar,
                    this.schedulingPlayer,
                    this.score,
                    this.slotSize,
                    this.solverStatus
            );
        }

        private void setupDateTimeSlot() {
            LocalDateTime startDateTime = this.schedulingWorkCalendar.getStartDateTime();
            LocalDateTime endDateTime = this.schedulingWorkCalendar.getEndDateTime();
            List<SchedulingDateTimeSlot> schedulingDateTimeSlots
                    = SchedulingDateTimeSlot.toValueRange(
                    startDateTime,
                    endDateTime,
                    slotSize.getMinute()
            );
            schedulingDateTimeSlots(schedulingDateTimeSlots);
        }

        public void setupGameActions() {
            ArrangementIdRoller idRoller = ArrangementIdRoller.forProblem(this.uuid);

            var producingArrangementArrayList
                    = this.schedulingOrderList
                    .stream()
                    .map(SchedulingOrder::calcFactoryActions)
                    .flatMap(Collection::stream)
                    .map(productAction -> expandAndSetupIntoMaterials(idRoller, productAction))
                    .flatMap(Collection::stream)
                    .peek(SchedulingProducingArrangement::readyElseThrow)
                    .collect(Collectors.toCollection(ArrayList::new));

            schedulingProducingArrangementList(producingArrangementArrayList);
        }

        private void trimUnrelatedObject() {
            List<SchedulingProduct> relatedSchedulingProduct
                    = this.schedulingProducingArrangementList.stream()
                    .map(SchedulingProducingArrangement::getSchedulingProduct)
                    .toList();
            this.schedulingProductList.removeIf(product -> !relatedSchedulingProduct.contains(product));

            List<SchedulingFactoryInfo> relatedSchedulingFactoryInfo
                    = this.schedulingProducingArrangementList.stream()
                    .map(SchedulingProducingArrangement::getRequiredFactoryInfo)
                    .toList();

            this.schedulingFactoryInfoList.removeIf(
                    schedulingFactoryInfo -> {
                        boolean anyMatch = relatedSchedulingFactoryInfo.stream()
                                .anyMatch(streamIterating -> {
                                    return streamIterating.getCategoryName()
                                            .equals(schedulingFactoryInfo.getCategoryName());
                                });
                        return !anyMatch;
                    }
            );
            this.schedulingFactoryInstanceList.removeIf(
                    factory -> {
                        SchedulingFactoryInfo schedulingFactoryInfo = factory.getSchedulingFactoryInfo();
                        boolean anyMatch = relatedSchedulingFactoryInfo.stream()
                                .anyMatch(streamIterating -> {
                                    boolean categoryEqual = streamIterating.getCategoryName()
                                            .equals(schedulingFactoryInfo.getCategoryName());
                                    return categoryEqual;
                                });
                        return !anyMatch;
                    }
            );
        }

        private TownshipSchedulingProblemBuilder schedulingDateTimeSlots(List<SchedulingDateTimeSlot> schedulingDateTimeSlots) {
            this.schedulingDateTimeSlots = schedulingDateTimeSlots;
            return this;
        }

        private ArrayList<SchedulingProducingArrangement> expandAndSetupIntoMaterials(
                ArrangementIdRoller idRoller,
                SchedulingProducingArrangement producingArrangement
        ) {
            LinkedList<SchedulingProducingArrangement> dealingChain = new LinkedList<>(List.of(producingArrangement));
            ArrayList<SchedulingProducingArrangement> resultArrangementList = new ArrayList<>();

            while (!dealingChain.isEmpty()) {
                SchedulingProducingArrangement iteratingArrangement
                        = dealingChain.removeFirst();
                iteratingArrangement.activate(idRoller, this.schedulingWorkCalendar, this.schedulingPlayer);
                resultArrangementList.add(iteratingArrangement);

                Set<SchedulingProducingExecutionMode> executionModes
                        = iteratingArrangement.getCurrentActionObject().getExecutionModeSet();
                SchedulingProducingExecutionMode producingExecutionMode
                        = executionModes.stream()
                        .min(Comparator.comparing(SchedulingProducingExecutionMode::getExecuteDuration))
                        .orElseThrow();
                iteratingArrangement.setProducingExecutionMode(producingExecutionMode);

                List<SchedulingProducingArrangement> materialsActions
                        = producingExecutionMode.materialsActions();
                iteratingArrangement.appendPrerequisiteArrangements(materialsActions);
                for (SchedulingProducingArrangement materialsAction : materialsActions) {
                    dealingChain.addLast(materialsAction);
                }

            }

            return resultArrangementList;
        }

        private TownshipSchedulingProblemBuilder schedulingProducingArrangementList(List<SchedulingProducingArrangement> schedulingProducingArrangementList) {
            this.schedulingProducingArrangementList = schedulingProducingArrangementList;
            return this;
        }

        public String toString() {
            return "TownshipSchedulingProblem.TownshipSchedulingProblemBuilder(uuid=" + this.uuid + ", schedulingProductList=" + this.schedulingProductList + ", schedulingFactoryInfoList=" + this.schedulingFactoryInfoList + ", schedulingOrderList=" + this.schedulingOrderList + ", schedulingFactoryInstanceList=" + this.schedulingFactoryInstanceList + ", schedulingDateTimeSlots=" + this.schedulingDateTimeSlots + ", schedulingProducingArrangementList=" + this.schedulingProducingArrangementList + ", schedulingWorkCalendar=" + this.schedulingWorkCalendar + ", schedulingPlayer=" + this.schedulingPlayer + ", score=" + this.score + ", slotSize=" + this.slotSize + ", solverStatus=" + this.solverStatus + ")";
        }

    }

}
