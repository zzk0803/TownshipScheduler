package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static final String FACTORY_QUEUE_VALUE_RANGE = "valueRangeForQueueFactories";

    public static final String FACTORY_SLOT_VALUE_RANGE = "valueRangeForSlotFactories";

    public static final String DATE_TIME_SLOT_VALUE_RANGE = "valueRangeForDateTimeSlot";

    public static final String PRODUCING_ARRANGEMENTS_FACTORY_SLOT_VALUE_RANGE
            = "valueRangeForSlotProducingArrangements";

    public static final String PRODUCING_ARRANGEMENTS_FACTORY_QUEUE_VALUE_RANGE
            = "valueRangeForQueueProducingArrangements";

    private String uuid;

    @ProblemFactCollectionProperty
    private List<SchedulingProduct> schedulingProductSet;

    @ProblemFactCollectionProperty
    private List<SchedulingFactoryInfo> schedulingFactoryInfoSet;

    @ProblemFactCollectionProperty
    private List<SchedulingOrder> schedulingOrderSet;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = FACTORY_QUEUE_VALUE_RANGE)
    private List<SchedulingTypeQueueFactoryInstance> schedulingTypeQueueFactoryInstanceList;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = FACTORY_SLOT_VALUE_RANGE)
    private List<SchedulingTypeSlotFactoryInstance> schedulingTypeSlotFactoryInstanceList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = DATE_TIME_SLOT_VALUE_RANGE)
    private List<SchedulingDateTimeSlot> dateTimeSlotSet;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = PRODUCING_ARRANGEMENTS_FACTORY_QUEUE_VALUE_RANGE)
    private List<SchedulingFactoryQueueProducingArrangement> schedulingFactoryQueueProducingArrangements;

    @PlanningEntityCollectionProperty
    private List<SchedulingFactorySlotProducingArrangement> schedulingFactorySlotProducingArrangements;

    @ProblemFactProperty
    private SchedulingWorkTimeLimit schedulingWorkTimeLimit;

//    @ProblemFactCollectionProperty
//    @ValueRangeProvider(id = "producingExecutionMode")
//    private Set<SchedulingProducingExecutionMode> schedulingProducingExecutionModeSet;

    //    @PlanningEntityCollectionProperty
//    @ValueRangeProvider(id = "warehouseActions")
//    private List<SchedulingPlayerWarehouseAction> schedulingPlayerWarehouseActions;
//
//
    @ProblemFactProperty
    private SchedulingPlayer schedulingPlayer;

    @PlanningScore
    private HardMediumSoftScore score;

//    @PlanningScore(
//            bendableHardLevelsSize = BENDABLE_SCORE_HARD_SIZE,
//            bendableSoftLevelsSize = BENDABLE_SCORE_SOFT_SIZE
//    )
//    private BendableScore score;

    private DateTimeSlotSize slotSize;

    private SolverStatus solverStatus;

    public TownshipSchedulingProblem(
            String uuid,
            List<SchedulingProduct> schedulingProducts,
            List<SchedulingFactoryInfo> schedulingFactoryInfos,
            List<SchedulingOrder> schedulingOrders,
            List<SchedulingTypeQueueFactoryInstance> schedulingTypeQueueFactoryInstanceList,
            List<SchedulingTypeSlotFactoryInstance> schedulingTypeSlotFactoryInstanceList,
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
        this.schedulingTypeQueueFactoryInstanceList = schedulingTypeQueueFactoryInstanceList;
        this.schedulingTypeSlotFactoryInstanceList = schedulingTypeSlotFactoryInstanceList;
        this.schedulingPlayer = schedulingPlayer;
        this.schedulingWorkTimeLimit = schedulingWorkTimeLimit;
        this.slotSize = slotSize;
        this.setupDateTimeSlot();
        this.setupGameActions();
        this.trimUnrelatedObject();
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

    public void setupGameActions() {
        ActionIdRoller idRoller = ActionIdRoller.forProblem(getUuid());

        var producingArrangementArrayList
                = this.schedulingOrderSet
                .stream()
                .map(SchedulingOrder::calcFactoryActions)
                .flatMap(Collection::stream)
                .map(productAction -> expandAndSetupIntoMaterials(idRoller, productAction))
                .flatMap(Collection::stream)
                .peek(BaseProducingArrangement::readyElseThrow)
                .collect(Collectors.toCollection(ArrayList::new));

        List<SchedulingFactorySlotProducingArrangement> slotProducingArrangements = new ArrayList<>();
        List<SchedulingFactoryQueueProducingArrangement> queueProducingArrangements = new ArrayList<>();
        producingArrangementArrayList.forEach(baseProducingArrangement -> {
            if (baseProducingArrangement instanceof SchedulingFactorySlotProducingArrangement schedulingFactorySlotProducingArrangement) {
                slotProducingArrangements.add(schedulingFactorySlotProducingArrangement);
            } else if (baseProducingArrangement instanceof SchedulingFactoryQueueProducingArrangement schedulingFactoryQueueProducingArrangement) {
                queueProducingArrangements.add(schedulingFactoryQueueProducingArrangement);
            } else {
                Assert.isTrue(true, "another producingArrangement Type?!");
            }
        });
        setSchedulingFactorySlotProducingArrangements(slotProducingArrangements);
        setSchedulingFactoryQueueProducingArrangements(queueProducingArrangements);
    }

    private ArrayList<BaseProducingArrangement> expandAndSetupIntoMaterials(
            ActionIdRoller idRoller,
            BaseProducingArrangement producingArrangement
    ) {
        LinkedList<BaseProducingArrangement> dealingChain = new LinkedList<>(List.of(producingArrangement));
        ArrayList<BaseProducingArrangement> resultArrangementList = new ArrayList<>();

        while (!dealingChain.isEmpty()) {
            BaseProducingArrangement iteratingArrangement
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

            List<BaseProducingArrangement> materialsActions
                    = producingExecutionMode.materialsActions();
            iteratingArrangement.appendPrerequisiteArrangements(materialsActions);
            for (BaseProducingArrangement materialsAction : materialsActions) {
                materialsAction.appendSupportArrangements(List.of(iteratingArrangement));
                dealingChain.addLast(materialsAction);
            }

        }

        return resultArrangementList;
    }

    private void trimUnrelatedObject() {
        List<SchedulingProduct> relatedSchedulingProduct
                = getBaseProducingArrangements().stream()
                .map(BaseProducingArrangement::getSchedulingProduct)
                .toList();
        getSchedulingProductSet().removeIf(product -> !relatedSchedulingProduct.contains(product));

        List<SchedulingFactoryInfo> relatedSchedulingFactoryInfo
                = getBaseProducingArrangements().stream()
                .map(BaseProducingArrangement::requiredFactoryInfo)
                .toList();

        getSchedulingFactoryInfoSet().removeIf(
                schedulingFactoryInfo -> !relatedSchedulingFactoryInfo.contains(schedulingFactoryInfo)
        );
        getSchedulingTypeQueueFactoryInstanceList().removeIf(
                factory -> !relatedSchedulingFactoryInfo.contains(factory.getSchedulingFactoryInfo())
        );
        getSchedulingTypeSlotFactoryInstanceList().removeIf(
                factory -> !relatedSchedulingFactoryInfo.contains(factory.getSchedulingFactoryInfo())
        );
    }

    public List<BaseProducingArrangement> getBaseProducingArrangements() {
        List<BaseProducingArrangement> producingArrangements
                = new ArrayList<>(getSchedulingFactorySlotProducingArrangements());
        producingArrangements.addAll(getSchedulingFactoryQueueProducingArrangements());
        return producingArrangements;
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
