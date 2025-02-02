package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;
import org.springframework.util.Assert;
import zzk.townshipscheduler.backend.scheduling.model.utility.ActionIdRoller;
import zzk.townshipscheduler.backend.scheduling.ProductAmountBill;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Data
@PlanningSolution
public class TownshipSchedulingProblem {

    public static final int DEFAULT_SCHEDULING_TIMESLOT_SIZE = 15;

    public static final int BENDABLE_SCORE_HARD_SIZE = 2;

    public static final int BENDABLE_SCORE_SOFT_SIZE = 2;

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
    private Set<SchedulingGameActionExecutionMode> schedulingGameActionExecutionModeSet;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "factoryActions")
    private List<SchedulingPlayerFactoryAction> schedulingPlayerFactoryActions;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "warehouseActions")
    private List<SchedulingPlayerWarehouseAction> schedulingPlayerWarehouseActions;


    @PlanningEntityProperty
    private SchedulingWarehouse schedulingWarehouse;

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
            SchedulingWarehouse schedulingWarehouse,
            SchedulingWorkTimeLimit schedulingWorkTimeLimit,
            BendableScore score,
            SolverStatus solverStatus
    ) {
        this(
                schedulingProducts,
                schedulingFactoryInfos,
                schedulingOrders,
                schedulingFactoryInstances,
                schedulingWarehouse,
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
            SchedulingWarehouse schedulingWarehouse,
            SchedulingWorkTimeLimit schedulingWorkTimeLimit
    ) {
        this();
        this.schedulingProductSet = schedulingProducts;
        this.schedulingGameActionExecutionModeSet = schedulingProducts.stream()
                .flatMap(schedulingProduct -> schedulingProduct.getProducingExecutionModeSet().stream())
                .collect(Collectors.toSet());
        this.schedulingFactoryInfoSet = schedulingFactoryInfos;
        this.schedulingOrderSet = schedulingOrders;
        this.schedulingFactoryInstanceSet = schedulingFactoryInstances;
        this.schedulingWarehouse = schedulingWarehouse;
        this.schedulingWorkTimeLimit = schedulingWorkTimeLimit;

    }

    public TownshipSchedulingProblem() {
        this.uuid = UUID.randomUUID();
    }

    @ValueRangeProvider(id = "planningPlayerDoItDateTimeValueRange")
    public CountableValueRange<LocalDateTime> dateTimeValueRange() {
        return ValueRangeFactory.createLocalDateTimeValueRange(
                schedulingWorkTimeLimit.getStartDateTime(),
                schedulingWorkTimeLimit.getEndDateTime(),
                DEFAULT_SCHEDULING_TIMESLOT_SIZE,
                ChronoUnit.MINUTES
        );
    }

    @ValueRangeProvider(id = "warehouse")
    public List<SchedulingWarehouse> schedulingWarehouses() {
        return List.of(schedulingWarehouse);
    }

    public void setupGameActions() {
        ActionIdRoller idRoller = ActionIdRoller.createIdRoller();

        ArrayList<SchedulingPlayerFactoryAction> factoryActions
                = this.schedulingOrderSet
                .stream()
                .flatMap(
                        schedulingOrder -> schedulingOrder.calcFactoryActions().stream()
                )
                .flatMap(productAction ->
                        expandAndSetupGameActionSet(idRoller, productAction).stream()
                ).collect(Collectors.toCollection(ArrayList::new));
        factoryActions.forEach(SchedulingPlayerFactoryAction::readyElseThrow);

        ArrayList<SchedulingPlayerWarehouseAction> warehouseActions = this.schedulingOrderSet.stream()
                .flatMap(schedulingOrder -> schedulingOrder.calcWarehouseActions().stream())
                .peek(idRoller::setup)
                .collect(Collectors.toCollection(ArrayList::new));
        warehouseActions.forEach(SchedulingPlayerWarehouseAction::readyElseThrow);

        setSchedulingPlayerFactoryActions(factoryActions);
        setSchedulingPlayerWarehouseActions(warehouseActions);
    }

    private ArrayList<SchedulingPlayerFactoryAction> expandAndSetupGameActionSet(
            ActionIdRoller idRoller,
            SchedulingPlayerFactoryAction productAction
    ) {
        LinkedList<SchedulingPlayerFactoryAction> dealingChain = new LinkedList<>(List.of(productAction));
        ArrayList<SchedulingPlayerFactoryAction> result = new ArrayList<>();

        while (!dealingChain.isEmpty()) {
            SchedulingPlayerFactoryAction currentFactoryAction = dealingChain.removeFirst();
            currentFactoryAction.idRoller(idRoller);
            result.add(currentFactoryAction);

            Set<SchedulingGameActionExecutionMode> executionModes
                    = currentFactoryAction.getCurrentActionObject().getExecutionModeSet();

            if (executionModes.size() == 1) {
                SchedulingGameActionExecutionMode executionMode
                        = executionModes.toArray(SchedulingGameActionExecutionMode[]::new)[0];
                currentFactoryAction.forceSetupExecutionMode(executionMode);

                if (!executionMode.atomicProduct()) {
                    ProductAmountBill productAmountBill = executionMode.getMaterials();
                    for (Map.Entry<SchedulingProduct, Integer> entry : productAmountBill.entrySet()) {
                        SchedulingProduct material = entry.getKey();
                        Integer amount = entry.getValue();
                        for (int i = amount; i > 0; i--) {
                            List<SchedulingPlayerFactoryAction> listActionOfMaterial = material.calcFactoryActions();
                            for (SchedulingPlayerFactoryAction materialFactoryAction : listActionOfMaterial) {
                                Assert.notNull(materialFactoryAction, "materialFactoryAction shouldn't be null");
                                currentFactoryAction.biAssociateWholeToPart(materialFactoryAction);
                                dealingChain.addLast(materialFactoryAction);
                            }
                        }
                    }

                }
            }

        }
        return result;
    }


}
