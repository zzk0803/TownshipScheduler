package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;
import org.springframework.util.Assert;
import zzk.townshipscheduler.backend.scheduling.ProductAmountBill;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@PlanningSolution
public class TownshipSchedulingProblem {

    public static final int DEFAULT_SCHEDULING_TIMESLOT_SIZE = 15;

    private UUID uuid;

    @ProblemFactCollectionProperty
    private Set<SchedulingProduct> schedulingProductSet;

    @ProblemFactCollectionProperty
    private Set<SchedulingFactoryInfo> schedulingFactoryInfoSet;

    @ProblemFactCollectionProperty
    private Set<SchedulingOrder> schedulingOrderSet;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    private Set<SchedulingFactoryInstance> schedulingFactoryInstanceSet;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    private Set<SchedulingDateTimeSlot> dateTimeSlotSet;

    @PlanningEntityCollectionProperty
    private Set<SchedulingProducingExecutionMode> schedulingProducingExecutionModeSet;

    @PlanningEntityCollectionProperty
    private List<SchedulingGameAction> schedulingGameActions;

    @ProblemFactProperty
    private SchedulingWarehouse schedulingWarehouse;

    @ProblemFactProperty
    private SchedulingWorkTimeLimit schedulingWorkTimeLimit;

    @PlanningScore(
            bendableHardLevelsSize = 2,
            bendableSoftLevelsSize = 1
    )
    private BendableScore score;

    private SolverStatus solverStatus;

    public TownshipSchedulingProblem(
            UUID uuid,
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
        this.schedulingFactoryInfoSet = schedulingFactoryInfos;
        this.schedulingOrderSet = schedulingOrders;
        this.schedulingFactoryInstanceSet = schedulingFactoryInstances;
        this.schedulingWarehouse = schedulingWarehouse;
        this.schedulingWorkTimeLimit = schedulingWorkTimeLimit;

    }

    public TownshipSchedulingProblem() {
        this.uuid = UUID.randomUUID();
    }

    public void setupGameActions() {
        SchedulingGameAction.GameActionIdRoller idRoller = SchedulingGameAction.createIdRoller();
        SchedulingGameAction sourceAction = SchedulingGameAction.createSourceAction();
        SchedulingGameAction sinkAction = SchedulingGameAction.createSinkAction();

        sourceAction.idRoller(idRoller);

        ArrayList<SchedulingGameAction> mappedAction = this.schedulingOrderSet
                .stream()
                .flatMap(
                        schedulingOrder -> schedulingOrder.getGameActionSet().stream()
                )
                .flatMap(
                        productAction -> expandAndSetupGameActionSet(
                                idRoller, productAction, sourceAction, sinkAction
                        ).stream()
                )
                .collect(Collectors.toCollection(ArrayList<SchedulingGameAction>::new));
        sinkAction.idRoller(idRoller);

        ArrayList<SchedulingGameAction> result = new ArrayList<>(mappedAction);
        result.addFirst(sourceAction);
        result.addLast(sinkAction);
        result.forEach(SchedulingGameAction::readyElseThrow);

        this.setSchedulingGameActions(result);
    }

    private ArrayList<SchedulingGameAction> expandAndSetupGameActionSet(
            SchedulingGameAction.GameActionIdRoller idRoller,
            SchedulingGameAction productAction,
            SchedulingGameAction sourceAction,
            SchedulingGameAction sinkAction
    ) {
        productAction.setSourceGameAction(sourceAction);
        productAction.setSinkGameAction(sinkAction);

        LinkedList<SchedulingGameAction> dealingChain = new LinkedList<>(List.of(productAction));
        ArrayList<SchedulingGameAction> result = new ArrayList<>();

        while (!dealingChain.isEmpty()) {
            SchedulingGameAction gameAction = dealingChain.removeFirst();
            gameAction.idRoller(idRoller);
            result.add(gameAction);

            if (gameAction instanceof SchedulingGameActionProductProducing producingAction) {
                Set<SchedulingProducingExecutionMode> executionModes
                        = producingAction.getValueRangeProducingExecutionModeSet();

                if (executionModes.size() == 1) {
                    SchedulingProducingExecutionMode executionMode
                            = executionModes.toArray(SchedulingProducingExecutionMode[]::new)[0];
                    producingAction.setExecutionModeMandatory(executionMode);

                    if (!executionMode.atomicProduct()) {
                        ProductAmountBill productAmountBill = executionMode.getMaterials();
                        for (Map.Entry<SchedulingProduct, Integer> entry : productAmountBill.entrySet()) {
                            SchedulingProduct material = entry.getKey();
                            Integer amount = entry.getValue();
                            for (int i = amount; i > 0; i--) {
                                List<SchedulingGameAction> listActionOfMaterial = material.getGameActionSet();
                                for (SchedulingGameAction materialGameAction : listActionOfMaterial) {
                                    Assert.notNull(materialGameAction, "materialGameAction shouldn't be null");
                                    producingAction.biAssociateWholeToPart(materialGameAction);
                                    dealingChain.addLast(materialGameAction);
                                }
                            }
                        }

                    }
                }
            }
        }
        return result;
    }

    public void setupDateTimeSlotSet() {
        setupDateTimeSlotSet(
                this.schedulingWorkTimeLimit.getStartDateTime(),
                this.schedulingWorkTimeLimit.getEndDateTime(),
                DEFAULT_SCHEDULING_TIMESLOT_SIZE
        );
    }

    private void setupDateTimeSlotSet(
            final LocalDateTime startInclusive,
            final LocalDateTime endExclusive,
            final int durationInMinute
    ) {
        Set<SchedulingDateTimeSlot> result = SchedulingDateTimeSlot.generate(
                startInclusive,
                endExclusive,
                durationInMinute
        );
        this.setDateTimeSlotSet(result);
    }


}
