package zzk.townshipscheduler.backend.scheduling.model;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.utility.UuidGenerator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class TownshipSchedulingProblemBuilder {

    private String uuid;

    private List<SchedulingProduct> schedulingProductList;

    private List<SchedulingFactoryInfo> schedulingFactoryInfoList;

    private List<SchedulingOrder> schedulingOrderList;

    private List<SchedulingFactoryInstance> schedulingFactoryInstanceList;

    private List<SchedulingDateTimeSlot> schedulingDateTimeSlots;

    private List<SchedulingFactoryInstanceDateTimeSlot> schedulingFactoryInstanceDateTimeSlotList;

    private List<SchedulingProducingArrangement> schedulingProducingArrangementList;

    private SchedulingWorkCalendar schedulingWorkCalendar;

    private SchedulingPlayer schedulingPlayer;

    private BendableLongScore score;

    private DateTimeSlotSize slotSize;

    private SolverStatus solverStatus;

    TownshipSchedulingProblemBuilder() {
    }

    public TownshipSchedulingProblemBuilder uuid() {
        this.uuid = UuidGenerator.timeOrderedV6().toString();
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

    public TownshipSchedulingProblemBuilder score(BendableLongScore score) {
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
        this.setupFactoryDateTimeSlot();

        int orderSize = this.schedulingOrderList.size();
        long orderItemProducingArrangementCount = this.schedulingProducingArrangementList.stream()
                .filter(SchedulingProducingArrangement::isOrderDirect)
                .count();
        int totalItemProducingArrangementCount = this.schedulingProducingArrangementList.size();
        int dateTimeValueRangeCount = this.schedulingDateTimeSlots.size();
        int factoryCount = this.schedulingFactoryInstanceList.size();
        log.info(
                "your township scheduling problem include {} order,contain {} final product item to make,and include all materials  need {} arrangement.factory value range size:{},date times slot size:{}",
                orderSize,
                orderItemProducingArrangementCount,
                totalItemProducingArrangementCount,
                factoryCount,
                dateTimeValueRangeCount
        );

        return new TownshipSchedulingProblem(
                this.uuid,
                this.schedulingProductList,
                this.schedulingFactoryInfoList,
                this.schedulingOrderList,
                this.schedulingFactoryInstanceList,
                this.schedulingDateTimeSlots,
                this.schedulingFactoryInstanceDateTimeSlotList,
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

    private TownshipSchedulingProblemBuilder schedulingDateTimeSlots(List<SchedulingDateTimeSlot> schedulingDateTimeSlots) {
        this.schedulingDateTimeSlots = schedulingDateTimeSlots;
        return this;
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

    private ArrayList<SchedulingProducingArrangement> expandAndSetupIntoMaterials(
            ArrangementIdRoller idRoller,
            SchedulingProducingArrangement producingArrangement
    ) {
        SchedulingOrder arrangementSchedulingOrder = producingArrangement.getSchedulingOrder();
        LinkedList<SchedulingProducingArrangement> dealingChain = new LinkedList<>(List.of(producingArrangement));
        ArrayList<SchedulingProducingArrangement> resultArrangementList = new ArrayList<>();

        while (!dealingChain.isEmpty()) {
            SchedulingProducingArrangement iteratingArrangement
                    = dealingChain.removeFirst();
            iteratingArrangement.activate(idRoller, this.schedulingWorkCalendar, this.schedulingPlayer);
            resultArrangementList.add(iteratingArrangement);

            SchedulingProducingExecutionMode producingExecutionMode
                    = iteratingArrangement.getCurrentActionObject().getExecutionModeSet().stream()
                    .min(Comparator.comparing(SchedulingProducingExecutionMode::getExecuteDuration))
                    .orElseThrow();
            iteratingArrangement.setProducingExecutionMode(producingExecutionMode);

            if (producingArrangement.isOrderDirect()) {
                iteratingArrangement.setSchedulingOrderProduct(producingArrangement.getSchedulingProduct());
                iteratingArrangement.setSchedulingOrderProductArrangementId(producingArrangement.getId());
            }

            List<SchedulingProducingArrangement> materialsActions
                    = producingExecutionMode.materialsActions();
            iteratingArrangement.appendPrerequisiteArrangements(materialsActions);
            for (SchedulingProducingArrangement materialsAction : materialsActions) {
                materialsAction.setSchedulingOrder(arrangementSchedulingOrder);
                dealingChain.addLast(materialsAction);
            }

        }

        return resultArrangementList;
    }

    private TownshipSchedulingProblemBuilder schedulingProducingArrangementList(List<SchedulingProducingArrangement> schedulingProducingArrangementList) {
        this.schedulingProducingArrangementList = schedulingProducingArrangementList;
        return this;
    }

    private void setupFactoryDateTimeSlot() {
        AtomicInteger idRoller = new AtomicInteger(1);
        List<SchedulingFactoryInstanceDateTimeSlot> schedulingFactoryInstanceDateTimeSlots = new ArrayList<>();
        for (SchedulingFactoryInstance schedulingFactoryInstance : this.schedulingFactoryInstanceList) {
            NavigableSet<SchedulingFactoryInstanceDateTimeSlot> factoryInstanceDateTimeSlots = new TreeSet<>();
            SchedulingFactoryInstanceDateTimeSlot previous = null;
            for (SchedulingDateTimeSlot schedulingDateTimeSlot : this.schedulingDateTimeSlots) {
                SchedulingFactoryInstanceDateTimeSlot schedulingFactoryInstanceDateTimeSlot
                        = new SchedulingFactoryInstanceDateTimeSlot(
                        idRoller.getAndIncrement(),
                        schedulingFactoryInstance,
                        schedulingDateTimeSlot
                );
                if (previous != null) {
                    previous.setNext(schedulingFactoryInstanceDateTimeSlot);
                    schedulingFactoryInstanceDateTimeSlot.setPrevious(previous);
                }
                previous = schedulingFactoryInstanceDateTimeSlot;
                schedulingFactoryInstanceDateTimeSlots.add(schedulingFactoryInstanceDateTimeSlot);
                factoryInstanceDateTimeSlots.add(schedulingFactoryInstanceDateTimeSlot);
            }
            schedulingFactoryInstance.getSchedulingFactoryInstanceDateTimeSlots().clear();
            schedulingFactoryInstance.getSchedulingFactoryInstanceDateTimeSlots().addAll(factoryInstanceDateTimeSlots);
        }
        this.schedulingFactoryInstanceDateTimeSlotList = schedulingFactoryInstanceDateTimeSlots;
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

    public String toString() {
        return "TownshipSchedulingProblem.TownshipSchedulingProblemBuilder(uuid=" + this.uuid + ", schedulingProductList=" + this.schedulingProductList + ", schedulingFactoryInfoList=" + this.schedulingFactoryInfoList + ", schedulingOrderList=" + this.schedulingOrderList + ", schedulingFactoryInstanceList=" + this.schedulingFactoryInstanceList + ", schedulingDateTimeSlots=" + this.schedulingDateTimeSlots + ", schedulingProducingArrangementList=" + this.schedulingProducingArrangementList + ", schedulingWorkCalendar=" + this.schedulingWorkCalendar + ", schedulingPlayer=" + this.schedulingPlayer + ", score=" + this.score + ", slotSize=" + this.slotSize + ", solverStatus=" + this.solverStatus + ")";
    }

}
