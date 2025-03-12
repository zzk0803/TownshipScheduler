package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@PlanningSolution
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

    private UUID uuid;

    @ProblemFactCollectionProperty
    private List<SchedulingProduct> schedulingProductSet;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInfo> schedulingFactoryInfoSet;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> schedulingOrderSet;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInstanceSingle> schedulingFactoryInstanceSingleSet;

    @PlanningEntityCollectionProperty
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
    private SchedulingWarehouse schedulingWarehouse;

//    @PlanningScore(
//            bendableHardLevelsSize = BENDABLE_SCORE_HARD_SIZE,
//            bendableSoftLevelsSize = BENDABLE_SCORE_SOFT_SIZE
//    )
//    private BendableScore score;

    @PlanningScore
    private HardSoftScore score;

    private SolverStatus solverStatus;

    public TownshipSchedulingProblem(
            List<SchedulingProduct> schedulingProducts,
            List<SchedulingFactoryInfo> schedulingFactoryInfos,
            List<SchedulingOrder> schedulingOrders,
            List<SchedulingFactoryInstanceSingle> schedulingFactoryInstanceSingles,
            List<SchedulingFactoryInstanceMultiple> schedulingFactoryInstanceMultipleLists,
            SchedulingWarehouse schedulingWarehouse,
            SchedulingWorkTimeLimit schedulingWorkTimeLimit,
            DateTimeSlotSize slotSize,
            HardSoftScore score,
//            BendableScore score,
            SolverStatus solverStatus
    ) {
        this(
                schedulingProducts,
                schedulingFactoryInfos,
                schedulingOrders,
                schedulingFactoryInstanceSingles,
                schedulingFactoryInstanceMultipleLists,
                schedulingWarehouse,
                schedulingWorkTimeLimit,
                slotSize
        );
        this.score = score;
        this.solverStatus = solverStatus;
    }

    public TownshipSchedulingProblem(
            List<SchedulingProduct> schedulingProducts,
            List<SchedulingFactoryInfo> schedulingFactoryInfos,
            List<SchedulingOrder> schedulingOrders,
            List<SchedulingFactoryInstanceSingle> schedulingFactoryInstanceSingles,
            List<SchedulingFactoryInstanceMultiple> schedulingFactoryInstanceMultipleList,
            SchedulingWarehouse schedulingWarehouse,
            SchedulingWorkTimeLimit schedulingWorkTimeLimit,
            DateTimeSlotSize slotSize
    ) {
        this();
        this.schedulingProductSet = schedulingProducts;
//        this.schedulingProducingExecutionModeSet = schedulingProducts.stream()
//                .flatMap(schedulingProduct -> schedulingProduct.getExecutionModeSet().stream())
//                .collect(Collectors.toSet());
        this.schedulingFactoryInfoSet = schedulingFactoryInfos;
        this.schedulingOrderSet = schedulingOrders;
        this.schedulingFactoryInstanceSingleSet = schedulingFactoryInstanceSingles;
        this.schedulingFactoryInstanceMultipleSet = schedulingFactoryInstanceMultipleList;
        this.schedulingWarehouse = schedulingWarehouse;
        this.schedulingWorkTimeLimit = schedulingWorkTimeLimit;
        this.slotSize = slotSize;
        this.setupDateTimeSlot();
        this.setupGameActions();
    }

    public TownshipSchedulingProblem() {
        this.uuid = UUID.randomUUID();
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

//        ArrayList<SchedulingPlayerWarehouseAction> warehouseActions
//                = this.schedulingOrderSet.stream()
//                .flatMap(schedulingOrder -> schedulingOrder.calcWarehouseActions().stream())
//                .peek(activate::setup)
//                .collect(Collectors.toCollection(ArrayList::new));

        ArrayList<AbstractPlayerProducingArrangement> producingArrangementArrayList
                = this.schedulingOrderSet
                .stream()
                .flatMap(
                        schedulingOrder -> schedulingOrder.calcFactoryActions().stream()
                )
                .map(productAction -> expandAndSetupGameAction(idRoller, productAction))
                .flatMap(Collection::stream)
                .peek(AbstractPlayerProducingArrangement::readyElseThrow)
                .collect(Collectors.toCollection(ArrayList::new));

        setPlayerProducingArrangements(producingArrangementArrayList);
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

    private ArrayList<AbstractPlayerProducingArrangement> expandAndSetupGameAction(
//    private Pair<ArrayList<SchedulingPlayerFactoryAction>, ArrayList<SchedulingPlayerWarehouseAction>> expandAndSetupGameAction(
            ActionIdRoller idRoller,
            AbstractPlayerProducingArrangement productAction
    ) {
        LinkedList<AbstractPlayerProducingArrangement> dealingChain = new LinkedList<>(List.of(productAction));
        ArrayList<AbstractPlayerProducingArrangement> factoryActions = new ArrayList<>();
//        ArrayList<SchedulingPlayerWarehouseAction> warehouseActions = new ArrayList<>();

        while (!dealingChain.isEmpty()) {
            AbstractPlayerProducingArrangement currentFactoryAction = dealingChain.removeFirst();
            currentFactoryAction.activate(idRoller, this.schedulingWorkTimeLimit, this.schedulingWarehouse);
            factoryActions.add(currentFactoryAction);

            Set<SchedulingProducingExecutionMode> executionModes
                    = currentFactoryAction.getCurrentActionObject().getExecutionModeSet();
            SchedulingProducingExecutionMode producingExecutionMode
                    = executionModes.stream()
                    .min(Comparator.comparing(SchedulingProducingExecutionMode::getExecuteDuration))
                    .orElseThrow();
            currentFactoryAction.setProducingExecutionMode(producingExecutionMode);

            List<AbstractPlayerProducingArrangement> materialsActions = producingExecutionMode.materialsActions();
            materialsActions.forEach(materialsAction -> {
//                currentFactoryAction.biAssociateWholeToPart(materialsAction);
                dealingChain.addLast(materialsAction);
            });

//            if (executionModes.size() == 1) {
//                SchedulingProducingExecutionMode executionMode
//                        = executionModes.toArray(SchedulingProducingExecutionMode[]::new)[0];
//                currentFactoryAction.forceSetupExecutionMode(executionMode);
//                List<SchedulingPlayerFactoryAction> materialsActions = executionMode.materialsActions();
//                materialsActions.forEach(materialsAction -> {
//                    currentFactoryAction.biAssociateWholeToPart(materialsAction);
//                    dealingChain.addLast(materialsAction);
//                });

//                if (!executionMode.boolAtomicProduct()) {
//                    ProductAmountBill productAmountBill = executionMode.getMaterials();
//                    for (Map.Entry<SchedulingProduct, Integer> entry : productAmountBill.entrySet()) {
//                        SchedulingProduct material = entry.getKey();
//                        Integer amount = entry.getValue();
//                        for (int i = amount; i > 0; i--) {
//                            List<SchedulingPlayerFactoryAction> listFactoryActionOfMaterial = material.calcFactoryActions();
//                            List<SchedulingPlayerWarehouseAction> listWarehouseActionOfMaterial = material.calcWarehouseActions();
//                            warehouseActions.addAll(listWarehouseActionOfMaterial);
//                            for (SchedulingPlayerFactoryAction materialFactoryAction : listFactoryActionOfMaterial) {
//                                Assert.notNull(materialFactoryAction, "materialFactoryAction shouldn't be null");
//                                currentFactoryAction.biAssociateWholeToPart(materialFactoryAction);
//                                dealingChain.addLast(materialFactoryAction);
//                            }
//                        }
//                    }
//
//                }

        }

        return factoryActions;
    }

    public List<SchedulingPlayerProducingArrangement> getSchedulingPlayerProducingArrangement() {
        return this.playerProducingArrangements.stream()
                .filter(producingArrangement -> producingArrangement instanceof SchedulingPlayerProducingArrangement)
                .map(producingArrangement -> ((SchedulingPlayerProducingArrangement) producingArrangement))
                .toList();
    }

    public List<SchedulingPlayerProducingArrangement> getSchedulingPlayerProducingArrangement(SchedulingFactoryInstanceSingle schedulingFactoryInstanceSingle) {
        return this.playerProducingArrangements.stream()
                .filter(producingArrangement -> producingArrangement instanceof SchedulingPlayerProducingArrangement)
                .filter(producingArrangement -> producingArrangement.getFactory()==schedulingFactoryInstanceSingle)
                .map(producingArrangement -> ((SchedulingPlayerProducingArrangement) producingArrangement))
                .toList();
    }

    public List<SchedulingPlayerFactoryProducingArrangement> getSchedulingPlayerProducingFactoryArrangement() {
        return this.playerProducingArrangements.stream()
                .filter(producingArrangement -> producingArrangement instanceof SchedulingPlayerFactoryProducingArrangement)
                .map(producingArrangement -> ((SchedulingPlayerFactoryProducingArrangement) producingArrangement))
                .toList();
    }

    @ValueRangeProvider(id = SEQUENCE_VALUE_RANGE)
    private CountableValueRange<Integer> valueRangeForSequence() {
        return ValueRangeFactory.createIntValueRange(1, getPlayerProducingArrangements().size());
    }

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
