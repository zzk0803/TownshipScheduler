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

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = VALUE_RANGE_FOR_FACTORIES)
    private List<SchedulingFactoryInstance> schedulingFactoryInstanceList;

    @PlanningEntityCollectionProperty
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
            List<SchedulingProduct> schedulingProducts,
            List<SchedulingFactoryInfo> schedulingFactoryInfos,
            List<SchedulingOrder> schedulingOrders,
            List<SchedulingFactoryInstance> schedulingFactoryInstanceList,
            SchedulingPlayer schedulingPlayer,
            SchedulingWorkCalendar schedulingWorkCalendar,
            DateTimeSlotSize slotSize
    ) {
        this.uuid = uuid;
        this.schedulingProductList = schedulingProducts;
        this.schedulingFactoryInfoList = schedulingFactoryInfos;
        this.schedulingOrderList = schedulingOrders;
        this.schedulingFactoryInstanceList = schedulingFactoryInstanceList;
        this.schedulingPlayer = schedulingPlayer;
        this.schedulingWorkCalendar = schedulingWorkCalendar;
        this.slotSize = slotSize;
        this.setupDateTimeSlot();
        this.setupGameActions();
        this.trimUnrelatedObject();
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
        setSchedulingDateTimeSlots(schedulingDateTimeSlots);
    }

    public void setupGameActions() {
        ActionIdRoller idRoller = ActionIdRoller.forProblem(getUuid());

        var producingArrangementArrayList
                = this.schedulingOrderList
                .stream()
                .map(SchedulingOrder::calcFactoryActions)
                .flatMap(Collection::stream)
                .map(productAction -> expandAndSetupIntoMaterials(idRoller, productAction))
                .flatMap(Collection::stream)
                .peek(SchedulingProducingArrangement::readyElseThrow)
                .collect(Collectors.toCollection(ArrayList::new));

        setSchedulingProducingArrangementList(producingArrangementArrayList);
    }

    private void trimUnrelatedObject() {
        List<SchedulingProduct> relatedSchedulingProduct
                = getSchedulingProducingArrangementList().stream()
                .map(SchedulingProducingArrangement::getSchedulingProduct)
                .toList();
        getSchedulingProductList().removeIf(product -> !relatedSchedulingProduct.contains(product));

        List<SchedulingFactoryInfo> relatedSchedulingFactoryInfo
                = getSchedulingProducingArrangementList().stream()
                .map(SchedulingProducingArrangement::getRequiredFactoryInfo)
                .toList();

        getSchedulingFactoryInfoList().removeIf(
                schedulingFactoryInfo -> !relatedSchedulingFactoryInfo.contains(schedulingFactoryInfo)
        );
        getSchedulingFactoryInstanceList().removeIf(
                factory -> !relatedSchedulingFactoryInfo.contains(factory.getSchedulingFactoryInfo())
        );
    }

    private ArrayList<SchedulingProducingArrangement> expandAndSetupIntoMaterials(
            ActionIdRoller idRoller,
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

//    @ProblemFactCollectionProperty
//    public List<FactoryProcessSequence> cacheFactoryProcessSequence() {
//        List<SchedulingProducingArrangement> producingArrangements = getSchedulingProducingArrangementList();
//        List<SchedulingDateTimeSlot> dateTimeSlots = getSchedulingDateTimeSlots();
//        return producingArrangements.stream()
//                .flatMap(schedulingProducingArrangement ->
//                        dateTimeSlots.stream()
//                                .map(schedulingDateTimeSlot -> new FactoryProcessSequence(
//                                        schedulingProducingArrangement,
//                                        schedulingDateTimeSlot
//                                ))
//                )
//                .toList();
//    }

//    @ProblemFactCollectionProperty
//    public List<ProducingArrangementConsequence> cacheFactProducingArrangementConsequenceList() {
//        return getSchedulingProducingArrangementList().stream()
//                .map(SchedulingProducingArrangement::calcConsequence)
//                .flatMap(Collection::stream)
//                .toList();
//    }

//        @ValueRangeProvider(id = "planningPlayerArrangeDateTimeValueRange")
//        public CountableValueRange<LocalDateTime> dateTimeValueRange() {
//            return ValueRangeFactory.createLocalDateTimeValueRange(
//                    schedulingWorkTimeLimit.getStartDateTime(),
//                    schedulingWorkTimeLimit.getEndDateTime(),
//                    getSlotSize().minute,
//                    ChronoUnit.MINUTES
//            );
//        }


}
