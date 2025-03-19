package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
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

    public static final int BENDABLE_SCORE_HARD_SIZE = 3;

    public static final int BENDABLE_SCORE_SOFT_SIZE = 2;

    public static final int HARD_BROKEN_QUEUE = 0;

    public static final int HARD_BROKEN_STOCK = 1;

    public static final int HARD_BAD_ASSIGN = 2;

    public static final int SOFT_MAKE_SPAN = 0;

    public static final int SOFT_BATTER = 1;

    public static final String FACTORY_VALUE_RANGE = "valueRangeForFactories";

    public static final String DATE_TIME_SLOT_VALUE_RANGE = "valueRangeForDateTimeSlot";

    public static final String SEQUENCE_VALUE_RANGE = "valueRangeForSequence";

    private String uuid;

    @ProblemFactCollectionProperty
    private List<SchedulingProduct> schedulingProductSet;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInfo> schedulingFactoryInfoSet;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> schedulingOrderSet;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInstanceSingle> schedulingFactoryInstanceSingleSet;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInstanceMultiple> schedulingFactoryInstanceMultipleSet;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = DATE_TIME_SLOT_VALUE_RANGE)
    private List<SchedulingDateTimeSlot> dateTimeSlotSet;

    private DateTimeSlotSize slotSize;

    @PlanningEntityCollectionProperty
    private List<AbstractPlayerProducingArrangement> playerProducingArrangements;

//    @ProblemFactCollectionProperty
//    @ValueRangeProvider(id = "producingExecutionMode")
//    private Set<SchedulingProducingExecutionMode> schedulingProducingExecutionModeSet;

    @ProblemFactProperty
    private SchedulingWorkTimeLimit schedulingWorkTimeLimit;

    //    @PlanningEntityCollectionProperty
//    @ValueRangeProvider(id = "warehouseActions")
//    private List<SchedulingPlayerWarehouseAction> schedulingPlayerWarehouseActions;
//
//
    @ProblemFactProperty
    private SchedulingPlayer schedulingPlayer;

//    @PlanningScore(
//            bendableHardLevelsSize = BENDABLE_SCORE_HARD_SIZE,
//            bendableSoftLevelsSize = BENDABLE_SCORE_SOFT_SIZE
//    )
//    private BendableScore score;

    @PlanningScore
    private HardSoftScore score;

    private SolverStatus solverStatus;

    public TownshipSchedulingProblem(
            String uuid,
            List<SchedulingProduct> schedulingProducts,
            List<SchedulingFactoryInfo> schedulingFactoryInfos,
            List<SchedulingOrder> schedulingOrders,
            List<SchedulingFactoryInstanceSingle> schedulingFactoryInstanceSingles,
            List<SchedulingFactoryInstanceMultiple> schedulingFactoryInstanceMultipleList,
            SchedulingPlayer schedulingPlayer,
            SchedulingWorkTimeLimit schedulingWorkTimeLimit,
            DateTimeSlotSize slotSize
    ) {
        this.uuid = uuid;
        this.schedulingProductSet = schedulingProducts;
//        this.schedulingProducingExecutionModeSet = schedulingProducts.stream()
//                .flatMap(schedulingProduct -> schedulingProduct.getExecutionModeSet().stream())
//                .collect(Collectors.toSet());
        this.schedulingFactoryInfoSet = schedulingFactoryInfos;
        this.schedulingOrderSet = schedulingOrders;
        this.schedulingFactoryInstanceSingleSet = schedulingFactoryInstanceSingles;
        this.schedulingFactoryInstanceMultipleSet = schedulingFactoryInstanceMultipleList;
        this.schedulingPlayer = schedulingPlayer;
        this.schedulingWorkTimeLimit = schedulingWorkTimeLimit;
        this.slotSize = slotSize;
        this.setupDateTimeSlot();
        this.setupGameActions();
    }

    public TownshipSchedulingProblem(
            String uuid,
            List<SchedulingProduct> schedulingProductSet,
            List<SchedulingFactoryInfo> schedulingFactoryInfoSet,
            List<SchedulingOrder> schedulingOrderSet,
            List<SchedulingFactoryInstanceSingle> schedulingFactoryInstanceSingleSet,
            List<SchedulingFactoryInstanceMultiple> schedulingFactoryInstanceMultipleSet,
            List<SchedulingDateTimeSlot> dateTimeSlotSet,
            DateTimeSlotSize slotSize,
            List<AbstractPlayerProducingArrangement> playerProducingArrangements,
            SchedulingWorkTimeLimit schedulingWorkTimeLimit,
            SchedulingPlayer schedulingPlayer
    ) {
        this.uuid = uuid;
        this.schedulingProductSet = schedulingProductSet;
        this.schedulingFactoryInfoSet = schedulingFactoryInfoSet;
        this.schedulingOrderSet = schedulingOrderSet;
        this.schedulingFactoryInstanceSingleSet = schedulingFactoryInstanceSingleSet;
        this.schedulingFactoryInstanceMultipleSet = schedulingFactoryInstanceMultipleSet;
        this.dateTimeSlotSet = dateTimeSlotSet;
        this.slotSize = slotSize;
        this.playerProducingArrangements = playerProducingArrangements;
        this.schedulingWorkTimeLimit = schedulingWorkTimeLimit;
        this.schedulingPlayer = schedulingPlayer;
    }

    private void setupDateTimeSlot() {
        LocalDateTime startDateTime = this.schedulingWorkTimeLimit.getStartDateTime();
        LocalDateTime endDateTime = this.schedulingWorkTimeLimit.getEndDateTime();
        List<SchedulingDateTimeSlot> schedulingDateTimeSlots
                = SchedulingDateTimeSlot.toValueRange(
                startDateTime,
                endDateTime,
                slotSize.minute
        );
        setDateTimeSlotSet(schedulingDateTimeSlots);
    }

//    public void setupPeriodFactory() {
//        AtomicInteger idRoller = new AtomicInteger(1);
//        Set<SchedulingFactoryTimeSlotInstance> schedulingPeriodFactories = new LinkedHashSet<>();
//        Set<SchedulingFactoryInstance> factoryInstances = getSchedulingFactoryInstanceSet();
//        Set<SchedulingDateTimeSlot> timeSlotSet = getDateTimeSlotSet();
//        for (SchedulingFactoryInstance factoryInstance : factoryInstances) {
//            SchedulingFactoryTimeSlotInstance previousOfIterating = null;
//            for (SchedulingDateTimeSlot timeSlot : timeSlotSet) {
//                SchedulingFactoryTimeSlotInstance schedulingFactoryTimeSlotInstance = new SchedulingFactoryTimeSlotInstance();
//                schedulingFactoryTimeSlotInstance.setId(idRoller.getAndIncrement());
//                schedulingFactoryTimeSlotInstance.setFactoryInstance(factoryInstance);
//                schedulingFactoryTimeSlotInstance.setDateTimeSlot(timeSlot);
//                if (previousOfIterating != null) {
//                    schedulingFactoryTimeSlotInstance.setPreviousPeriodOfFactory(previousOfIterating);
//                    previousOfIterating.setNextPeriodOfFactory(schedulingFactoryTimeSlotInstance);
//                }
//                factoryInstance.addPeriodFactory(schedulingFactoryTimeSlotInstance);
//                schedulingPeriodFactories.add(schedulingFactoryTimeSlotInstance);
//                previousOfIterating = schedulingFactoryTimeSlotInstance;
//            }
//        }
//
//        setSchedulingFactoryTimeSlotInstanceSet(schedulingPeriodFactories);
//    }

    public void setupGameActions() {
        ActionIdRoller idRoller = ActionIdRoller.forProblem(getUuid().toString());

        ArrayList<AbstractPlayerProducingArrangement> producingArrangementArrayList
                = this.schedulingOrderSet
                .stream()
                .map(SchedulingOrder::calcFactoryActions)
                .flatMap(Collection::stream)
                .map(productAction -> expandAndSetupIntoMaterials(idRoller, productAction))
                .flatMap(Collection::stream)
                .peek(AbstractPlayerProducingArrangement::readyElseThrow)
                .collect(Collectors.toCollection(ArrayList::new));
        setPlayerProducingArrangements(producingArrangementArrayList);

        //setup EntitySizeEstimated
        producingArrangementArrayList.stream()
                .collect(Collectors.groupingBy(
                        AbstractPlayerProducingArrangement::getRequiredFactoryInfo,
                        Collectors.counting()
                ))
                .forEach((schedulingFactoryInfo, aLong) -> {
                    schedulingFactoryInfo.setEntitySizeEstimated(Math.toIntExact(aLong));
                });

        //trim unrelated entity
        List<SchedulingProduct> relatedSchedulingProduct
                = producingArrangementArrayList.stream()
                .map(AbstractPlayerProducingArrangement::getSchedulingProduct)
                .toList();
        getSchedulingProductSet().removeIf(product -> !relatedSchedulingProduct.contains(product));

        List<SchedulingFactoryInfo> relatedSchedulingFactoryInfo
                = producingArrangementArrayList.stream()
                .map(AbstractPlayerProducingArrangement::getRequiredFactoryInfo)
                .toList();
        getSchedulingFactoryInfoSet().removeIf(
                schedulingFactoryInfo -> !relatedSchedulingFactoryInfo.contains(schedulingFactoryInfo)
        );

        List<SchedulingFactoryInstanceSingle> relatedSchedulingFactoryInstanceSingle
                = producingArrangementArrayList.stream()
                .filter(producingArrangement -> producingArrangement instanceof SchedulingPlayerProducingArrangement)
                .map(producingArrangement -> ((SchedulingPlayerProducingArrangement) producingArrangement))
                .map(SchedulingPlayerProducingArrangement::getFactory)
                .toList();
        getSchedulingFactoryInstanceSingleSet().removeIf(factory -> {
            return !relatedSchedulingFactoryInstanceSingle.contains(factory);
        });

        var relatedSchedulingFactoryInstanceMultipleSet
                = producingArrangementArrayList.stream()
                .filter(producingArrangement -> producingArrangement instanceof SchedulingPlayerFactoryProducingArrangement)
                .map(producingArrangement -> ((SchedulingPlayerFactoryProducingArrangement) producingArrangement))
                .map(SchedulingPlayerFactoryProducingArrangement::getValueRangeForPlanningFactory)
                .flatMap(Collection::stream)
                .toList();
        getSchedulingFactoryInstanceMultipleSet().removeIf(factory -> {
            return !relatedSchedulingFactoryInstanceMultipleSet.contains(factory);
        });
    }

//        @ValueRangeProvider(id = "planningPlayerArrangeDateTimeValueRange")
//        public CountableValueRange<LocalDateTime> dateTimeValueRange() {
//            return ValueRangeFactory.createLocalDateTimeValueRange(
//                    schedulingWorkTimeLimit.getStartDateTime(),
//                    schedulingWorkTimeLimit.getEndDateTime(),
//                    getSlotSize().minute,
//                    ChronoUnit.MINUTES
//            );
//        }

    private ArrayList<AbstractPlayerProducingArrangement> expandAndSetupIntoMaterials(
            ActionIdRoller idRoller,
            AbstractPlayerProducingArrangement producingArrangement
    ) {
        LinkedList<AbstractPlayerProducingArrangement> dealingChain = new LinkedList<>(List.of(producingArrangement));
        ArrayList<AbstractPlayerProducingArrangement> resultArrangementList = new ArrayList<>();

        while (!dealingChain.isEmpty()) {
            AbstractPlayerProducingArrangement iteratingArrangement
                    = dealingChain.removeFirst();
            iteratingArrangement.activate(idRoller, this.schedulingWorkTimeLimit, this.schedulingPlayer);
            resultArrangementList.add(iteratingArrangement);

            Set<SchedulingProducingExecutionMode> executionModes
                    = iteratingArrangement.getCurrentActionObject().getExecutionModeSet();
            SchedulingProducingExecutionMode producingExecutionMode
                    = executionModes.stream()
                    .min(Comparator.comparing(SchedulingProducingExecutionMode::getExecuteDuration))
                    .orElseThrow();
            iteratingArrangement.setProducingExecutionMode(producingExecutionMode);

            List<AbstractPlayerProducingArrangement> materialsActions
                    = producingExecutionMode.materialsActions();
            iteratingArrangement.appendPrerequisiteArrangements(materialsActions);
            for (AbstractPlayerProducingArrangement materialsAction : materialsActions) {
                materialsAction.appendSupportArrangements(List.of(iteratingArrangement));
                dealingChain.addLast(materialsAction);
            }

        }

        return resultArrangementList;
    }

    public List<SchedulingPlayerProducingArrangement> getSchedulingPlayerProducingArrangement() {
        return this.playerProducingArrangements.stream()
                .filter(producingArrangement -> producingArrangement instanceof SchedulingPlayerProducingArrangement)
                .map(producingArrangement -> ((SchedulingPlayerProducingArrangement) producingArrangement))
                .toList();
    }

    public List<SchedulingPlayerFactoryProducingArrangement> getSchedulingPlayerFactoryProducingArrangement() {
        return this.playerProducingArrangements.stream()
                .filter(producingArrangement -> producingArrangement instanceof SchedulingPlayerFactoryProducingArrangement)
                .map(producingArrangement -> ((SchedulingPlayerFactoryProducingArrangement) producingArrangement))
                .toList();
    }

    public List<SchedulingPlayerFactoryProducingArrangement> getSchedulingPlayerFactoryProducingArrangement(
            AbstractFactoryInstance factoryInstance
    ) {
        return this.playerProducingArrangements.stream()
                .filter(producingArrangement -> producingArrangement instanceof SchedulingPlayerFactoryProducingArrangement)
                .map(producingArrangement -> ((SchedulingPlayerFactoryProducingArrangement) producingArrangement))
                .filter(producingArrangement -> producingArrangement.getFactory() == factoryInstance)
                .toList();
    }

    public List<SchedulingPlayerProducingArrangement> getSchedulingPlayerProducingArrangement(AbstractFactoryInstance factoryInstance) {
        return this.playerProducingArrangements.stream()
                .filter(producingArrangement -> producingArrangement instanceof SchedulingPlayerProducingArrangement)
                .filter(producingArrangement -> producingArrangement.getFactory() == factoryInstance)
                .map(producingArrangement -> ((SchedulingPlayerProducingArrangement) producingArrangement))
                .toList();
    }

    public List<SchedulingPlayerFactoryProducingArrangement> getSchedulingPlayerProducingFactoryArrangement() {
        return this.playerProducingArrangements.stream()
                .filter(producingArrangement -> producingArrangement instanceof SchedulingPlayerFactoryProducingArrangement)
                .map(producingArrangement -> ((SchedulingPlayerFactoryProducingArrangement) producingArrangement))
                .toList();
    }

//    @ValueRangeProvider(id = SEQUENCE_VALUE_RANGE)
//    private CountableValueRange<Integer> valueRangeForSequence() {
//        return ValueRangeFactory.createIntValueRange(1, getPlayerProducingArrangements().size());
//    }

    public enum DateTimeSlotSize {
        SMALL(10),
        HALF_HOUR(30),
        HOUR(60),
        BIG(180);

        private final int minute;

        DateTimeSlotSize(int minute) {
            this.minute = minute;
        }
    }


}
