package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Data
@PlanningSolution
public class TownshipSchedulingProblem {

    public static final int BENDABLE_SCORE_HARD_SIZE = 2;

    public static final int BENDABLE_SCORE_SOFT_SIZE = 2;

    public static final int HARD_BROKEN = 0;

    public static final int HARD_NEED_REVISE = 1;

    public static final int SOFT_MAKESPAN = 0;

    public static final int SOFT_BATTER = 1;

    private UUID uuid;

    @ProblemFactCollectionProperty
    private Set<SchedulingProduct> schedulingProductSet;

    @ProblemFactCollectionProperty
    private Set<SchedulingFactoryInfo> schedulingFactoryInfoSet;

    @ProblemFactCollectionProperty
    private Set<SchedulingOrder> schedulingOrderSet;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "factories")
    private Set<SchedulingFactoryInstance> schedulingFactoryInstanceSet;

//    @ProblemFactCollectionProperty
//    @ValueRangeProvider(id = "dateTimeSlot")
//    private Set<SchedulingDateTimeSlot> dateTimeSlotSet;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "producingExecutionMode")
    private Set<SchedulingProducingExecutionMode> schedulingProducingExecutionModeSet;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "factoryActions")
    private List<SchedulingPlayerFactoryAction> schedulingPlayerFactoryActions;

//    @PlanningEntityCollectionProperty
//    @ValueRangeProvider(id = "warehouseActions")
//    private List<SchedulingPlayerWarehouseAction> schedulingPlayerWarehouseActions;
//
//
//    @PlanningEntityProperty
//    private SchedulingWarehouse schedulingWarehouse;

    @ProblemFactProperty
    private SchedulingWorkTimeLimit schedulingWorkTimeLimit;

    @PlanningScore(
            bendableHardLevelsSize = BENDABLE_SCORE_HARD_SIZE,
            bendableSoftLevelsSize = BENDABLE_SCORE_SOFT_SIZE
    )
    private BendableScore score;

    private SolverStatus solverStatus;

    public TownshipSchedulingProblem(
            Set<SchedulingProduct> schedulingProducts,
            Set<SchedulingFactoryInfo> schedulingFactoryInfos,
            Set<SchedulingOrder> schedulingOrders,
            Set<SchedulingFactoryInstance> schedulingFactoryInstances,
//            SchedulingWarehouse schedulingWarehouse,
            SchedulingWorkTimeLimit schedulingWorkTimeLimit,
            BendableScore score,
            SolverStatus solverStatus
    ) {
        this(
                schedulingProducts,
                schedulingFactoryInfos,
                schedulingOrders,
                schedulingFactoryInstances,
//                schedulingWarehouse,
                schedulingWorkTimeLimit
        );
        this.score = score;
        this.solverStatus = solverStatus;
    }

    public TownshipSchedulingProblem(
            Set<SchedulingProduct> schedulingProducts,
            Set<SchedulingFactoryInfo> schedulingFactoryInfos,
            Set<SchedulingOrder> schedulingOrders,
            Set<SchedulingFactoryInstance> schedulingFactoryInstances,
//            SchedulingWarehouse schedulingWarehouse,
            SchedulingWorkTimeLimit schedulingWorkTimeLimit
    ) {
        this();
        this.schedulingProductSet = schedulingProducts;
        this.schedulingProducingExecutionModeSet = schedulingProducts.stream()
                .flatMap(schedulingProduct -> schedulingProduct.getExecutionModeSet().stream())
                .collect(Collectors.toSet());
        this.schedulingFactoryInfoSet = schedulingFactoryInfos;
        this.schedulingOrderSet = schedulingOrders;
        this.schedulingFactoryInstanceSet = schedulingFactoryInstances;
//        this.schedulingWarehouse = schedulingWarehouse;
        this.schedulingWorkTimeLimit = schedulingWorkTimeLimit;

    }

    public TownshipSchedulingProblem() {
        this.uuid = UUID.randomUUID();
    }

    @ValueRangeProvider(id = "planningPlayerArrangeDateTimeValueRange")
    public CountableValueRange<LocalDateTime> dateTimeValueRange() {
        return ValueRangeFactory.createLocalDateTimeValueRange(
                schedulingWorkTimeLimit.getStartDateTime(),
                schedulingWorkTimeLimit.getEndDateTime(),
                15,
                ChronoUnit.MINUTES
        );
    }

//    @ValueRangeProvider(id = "warehouse")
//    public List<SchedulingWarehouse> schedulingWarehouses() {
//        return List.of(schedulingWarehouse);
//    }

    public void setupGameActions() {
        ActionIdRoller idRoller = ActionIdRoller.forProblem(getUuid().toString());

//        ArrayList<SchedulingPlayerWarehouseAction> warehouseActions
//                = this.schedulingOrderSet.stream()
//                .flatMap(schedulingOrder -> schedulingOrder.calcWarehouseActions().stream())
//                .peek(idRoller::setup)
//                .collect(Collectors.toCollection(ArrayList::new));

        ArrayList<SchedulingPlayerFactoryAction> factoryActions = new ArrayList<>();
        this.schedulingOrderSet
                .stream()
                .flatMap(
                        schedulingOrder -> schedulingOrder.calcFactoryActions().stream()
                )
                .map(productAction -> expandAndSetupGameActionSet(idRoller, productAction))
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

    private ArrayList<SchedulingPlayerFactoryAction> expandAndSetupGameActionSet(
//    private Pair<ArrayList<SchedulingPlayerFactoryAction>, ArrayList<SchedulingPlayerWarehouseAction>> expandAndSetupGameActionSet(
            ActionIdRoller idRoller,
            SchedulingPlayerFactoryAction productAction
    ) {
        LinkedList<SchedulingPlayerFactoryAction> dealingChain = new LinkedList<>(List.of(productAction));
        ArrayList<SchedulingPlayerFactoryAction> factoryActions = new ArrayList<>();
//        ArrayList<SchedulingPlayerWarehouseAction> warehouseActions = new ArrayList<>();

        while (!dealingChain.isEmpty()) {
            SchedulingPlayerFactoryAction currentFactoryAction = dealingChain.removeFirst();
            currentFactoryAction.idRoller(idRoller);
            factoryActions.add(currentFactoryAction);

            Set<SchedulingProducingExecutionMode> executionModes
                    = currentFactoryAction.getCurrentActionObject().getExecutionModeSet();

            if (executionModes.size() == 1) {
                SchedulingProducingExecutionMode executionMode
                        = executionModes.toArray(SchedulingProducingExecutionMode[]::new)[0];
                currentFactoryAction.forceSetupExecutionMode(executionMode);
                List<SchedulingPlayerFactoryAction> materialsActions = executionMode.materialsActions();
                materialsActions.forEach(materialsAction -> {
                    currentFactoryAction.biAssociateWholeToPart(materialsAction);
                    dealingChain.addLast(materialsAction);
                });
//                if (!executionMode.atomicProduct()) {
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

        }
        return factoryActions;
//        return Pair.with(factoryActions, warehouseActions);
    }


}
