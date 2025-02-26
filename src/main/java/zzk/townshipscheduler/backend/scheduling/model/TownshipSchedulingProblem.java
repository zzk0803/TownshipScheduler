package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
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

    private UUID uuid;

    @ProblemFactCollectionProperty
    private Set<SchedulingProduct> schedulingProductSet;

    @ProblemFactCollectionProperty
    private Set<SchedulingFactoryInfo> schedulingFactoryInfoSet;

    @ProblemFactCollectionProperty
    private Set<SchedulingOrder> schedulingOrderSet;

    @PlanningEntityCollectionProperty
//    @ValueRangeProvider(id = "valueRangeForSchedulingFactoryInstance")
    private Set<SchedulingFactoryInstance> schedulingFactoryInstanceSet;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "planningPlayerArrangeDateTimeValueRange")
    private Set<SchedulingDateTimeSlot> dateTimeSlotSet;

    private DateTimeSlotSize slotSize;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "factoryActions")
    private List<SchedulingPlayerFactoryAction> schedulingPlayerFactoryActions;

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
            Set<SchedulingProduct> schedulingProducts,
            Set<SchedulingFactoryInfo> schedulingFactoryInfos,
            Set<SchedulingOrder> schedulingOrders,
            Set<SchedulingFactoryInstance> schedulingFactoryInstances,
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
                schedulingFactoryInstances,
                schedulingWarehouse,
                schedulingWorkTimeLimit,
                slotSize
        );
        this.score = score;
        this.solverStatus = solverStatus;
    }

    public TownshipSchedulingProblem(
            Set<SchedulingProduct> schedulingProducts,
            Set<SchedulingFactoryInfo> schedulingFactoryInfos,
            Set<SchedulingOrder> schedulingOrders,
            Set<SchedulingFactoryInstance> schedulingFactoryInstances,
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
        this.schedulingFactoryInstanceSet = schedulingFactoryInstances;
        this.schedulingWarehouse = schedulingWarehouse;
        this.schedulingWorkTimeLimit = schedulingWorkTimeLimit;
        this.slotSize = slotSize;
        this.setupDateTimeSlot();
        //        this.setupPeriodFactory();
        this.setupGameActions();
    }

    public TownshipSchedulingProblem() {
        this.uuid = UUID.randomUUID();
    }

    private void setupDateTimeSlot() {
        LocalDateTime startDateTime = this.schedulingWorkTimeLimit.getStartDateTime();
        LocalDateTime endDateTime = this.schedulingWorkTimeLimit.getEndDateTime();
        Set<SchedulingDateTimeSlot> schedulingDateTimeSlots
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

        ArrayList<SchedulingPlayerFactoryAction> factoryActions = new ArrayList<>();
        this.schedulingOrderSet
                .stream()
                .flatMap(
                        schedulingOrder -> schedulingOrder.calcFactoryActions().stream()
                )
                .map(productAction -> expandAndSetupGameAction(idRoller, productAction))
                .forEach(schedulingPlayerFactoryActions -> {
//                    ArrayList<SchedulingPlayerFactoryAction> collectingFactoryActions = schedulingPlayerFactoryActions.getValue0();
//                    ArrayList<SchedulingPlayerWarehouseAction> collectingWarehouseActions = schedulingPlayerFactoryActions.getValue1();
                    factoryActions.addAll(schedulingPlayerFactoryActions);
//                    warehouseActions.addAll(collectingWarehouseActions);
                });

        factoryActions.forEach(SchedulingPlayerFactoryAction::readyElseThrow);
//        warehouseActions.forEach(SchedulingPlayerWarehouseAction::readyElseThrow);

        setSchedulingPlayerFactoryActions(factoryActions);
//        setSchedulingPlayerWarehouseActions(warehouseActions);
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

    private ArrayList<SchedulingPlayerFactoryAction> expandAndSetupGameAction(
//    private Pair<ArrayList<SchedulingPlayerFactoryAction>, ArrayList<SchedulingPlayerWarehouseAction>> expandAndSetupGameAction(
            ActionIdRoller idRoller,
            SchedulingPlayerFactoryAction productAction
    ) {
        LinkedList<SchedulingPlayerFactoryAction> dealingChain = new LinkedList<>(List.of(productAction));
        ArrayList<SchedulingPlayerFactoryAction> factoryActions = new ArrayList<>();
//        ArrayList<SchedulingPlayerWarehouseAction> warehouseActions = new ArrayList<>();

        while (!dealingChain.isEmpty()) {
            SchedulingPlayerFactoryAction currentFactoryAction = dealingChain.removeFirst();
            currentFactoryAction.activate(idRoller, this.schedulingWorkTimeLimit, this.schedulingWarehouse);
            factoryActions.add(currentFactoryAction);

            Set<SchedulingProducingExecutionMode> executionModes
                    = currentFactoryAction.getCurrentActionObject().getExecutionModeSet();
            SchedulingProducingExecutionMode producingExecutionMode
                    = executionModes.stream()
                    .min(Comparator.comparing(SchedulingProducingExecutionMode::getExecuteDuration))
                    .orElseThrow();
            currentFactoryAction.setProducingExecutionMode(producingExecutionMode);

            List<SchedulingPlayerFactoryAction> materialsActions = producingExecutionMode.materialsActions();
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

    @ValueRangeProvider(id = "valueRangeForSequence")
    private ValueRange<Integer> valueRangeForSequence() {
        return ValueRangeFactory.createIntValueRange(0, getSchedulingPlayerFactoryActions().size());
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
