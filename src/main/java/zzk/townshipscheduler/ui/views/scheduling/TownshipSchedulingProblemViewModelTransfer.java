package zzk.townshipscheduler.ui.views.scheduling;

import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.vaadin.copilot.shaded.helger.collection.commons.CommonsConcurrentHashMap;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.ui.pojo.scheduling.*;
import zzk.townshipscheduler.ui.pojo.scheduling.reactive.ReactiveSchedulingProducingArrangementViewModel;
import zzk.townshipscheduler.ui.pojo.scheduling.reactive.ReactiveTownshipSchedulingProblemViewModel;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SpringComponent
@RouteScope
@RouteScopeOwner(SchedulingView.class)
public class TownshipSchedulingProblemViewModelTransfer {

    private final Map<SchedulingProduct, SchedulingProductViewModel> schedulingProductToViewModelMap = new ConcurrentHashMap<>();

    private final Map<SchedulingFactoryInfo, SchedulingFactoryInfoViewModel> schedulingFactoryInfoToViewModelMap = new CommonsConcurrentHashMap<>();

    private final Map<SchedulingFactoryInstance, SchedulingFactoryInstanceViewModel> schedulingFactoryInstanceToViewModelMap = new ConcurrentHashMap<>();

    private final Map<SchedulingOrder, SchedulingOrderViewModel> schedulingOrderToViewModelMap = new ConcurrentHashMap<>();

    private final Map<SchedulingProducingArrangement, SchedulingProducingArrangementViewModel> schedulingProducingArrangementToViewModelMap = new ConcurrentHashMap<>();

    private final Map<SchedulingProducingArrangement, ReactiveSchedulingProducingArrangementViewModel> schedulingProducingArrangementToReactiveViewModelMap = new ConcurrentHashMap<>();

    private final Map<SchedulingDateTimeSlot, SchedulingDateTimeSlotViewModel> schedulingDateTimeSlotToViewModelMap = new ConcurrentHashMap<>();

    private final AtomicReference<TownshipSchedulingProblem> townshipSchedulingProblemAtomicReference = new AtomicReference<>();

    public TownshipSchedulingProblemViewModelTransfer forOtherProblem(TownshipSchedulingProblem townshipSchedulingProblem) {
        TownshipSchedulingProblemViewModelTransfer schedulingViewRecordComponent = new TownshipSchedulingProblemViewModelTransfer();
        schedulingViewRecordComponent.townshipSchedulingProblemAtomicReference.set(townshipSchedulingProblem);
        return schedulingViewRecordComponent;
    }

    public ReactiveTownshipSchedulingProblemViewModel mapAndGetReactiveModel() {
        HardMediumSoftBigDecimalScore score = getTownshipSchedulingProblem().getScore();
        ListSignal<ReactiveSchedulingProducingArrangementViewModel> arrangementViewModelListSignal = new ListSignal<>();
        arrangementViewModelListSignal.insertAllLast(mapAndUpdateReactiveSchedulingProducingArrangementViewModel());
        return new ReactiveTownshipSchedulingProblemViewModel(
                getTownshipSchedulingProblem().getUuid(),
                mapAndGetSchedulingProductViewModel(),
                mapAndGetSchedulingFactoryInfoViewModel(),
                mapAndGetSchedulingOrderViewModel(),
                mapAndGetSchedulingFactoryInstanceViewModel(),
                mapAndGetSchedulingDateTimeSlotViewModel(),
                arrangementViewModelListSignal,
                mapAndGetSchedulingWorkCalendarViewModel(),
                getTownshipSchedulingProblem().getDateTimeSlotSize()
                        .getMinute(),
                mapAndGetSchedulingPlayerViewModel(),
                new ValueSignal<>(getTownshipSchedulingProblem().getSolverStatus()
                        .name()),
                new ValueSignal<>(Objects.isNull(score)
                        ? "N/A"
                        : score.toString()),
                new ValueSignal<>(Objects.nonNull(score) && score.isFeasible())
        );
    }

    public TownshipSchedulingProblemViewModel mapAndGet() {
        HardMediumSoftBigDecimalScore score = getTownshipSchedulingProblem().getScore();
        return new TownshipSchedulingProblemViewModel(
                getTownshipSchedulingProblem().getUuid(),
                mapAndGetSchedulingProductViewModel(),
                mapAndGetSchedulingFactoryInfoViewModel(),
                mapAndGetSchedulingOrderViewModel(),
                mapAndGetSchedulingFactoryInstanceViewModel(),
                mapAndGetSchedulingDateTimeSlotViewModel(),
                mapAndGetSchedulingProducingArrangementViewModel(),
                mapAndGetSchedulingWorkCalendarViewModel(),
                getTownshipSchedulingProblem().getDateTimeSlotSize()
                        .getMinute(),
                mapAndGetSchedulingPlayerViewModel(),
                getTownshipSchedulingProblem().getSolverStatus()
                        .name(),
                Objects.isNull(score)
                        ? "N/A"
                        : score.toString(),
                Objects.nonNull(score) && score.isFeasible()
        );
    }

    public ReactiveTownshipSchedulingProblemViewModel updateAndGetReactiveViewModel(
            TownshipSchedulingProblem townshipSchedulingProblem,
            ReactiveTownshipSchedulingProblemViewModel reactiveTownshipSchedulingProblemViewModel
    ) {
        this.setTownshipSchedulingProblem(townshipSchedulingProblem);
        if (reactiveTownshipSchedulingProblemViewModel == null) {
            return mapAndGetReactiveModel();
        }

        Collection<ReactiveSchedulingProducingArrangementViewModel> reactiveSchedulingProducingArrangementViewModels
                = mapAndUpdateReactiveSchedulingProducingArrangementViewModel();
        SolverStatus solverStatus = getTownshipSchedulingProblem().getSolverStatus();
        HardMediumSoftBigDecimalScore score = getTownshipSchedulingProblem().getScore();
        reactiveTownshipSchedulingProblemViewModel.solverStatus().set(solverStatus.name());
        reactiveTownshipSchedulingProblemViewModel.score().set(
                Objects.isNull(score)
                        ? "N/A"
                        : score.toString()
        );
        reactiveTownshipSchedulingProblemViewModel.feasible().set(Objects.nonNull(score) && score.isFeasible());
        return reactiveTownshipSchedulingProblemViewModel;
    }

    public TownshipSchedulingProblem getTownshipSchedulingProblem() {
        return townshipSchedulingProblemAtomicReference.get();
    }

    public void setTownshipSchedulingProblem(TownshipSchedulingProblem newValue) {
        townshipSchedulingProblemAtomicReference.set(newValue);
    }

    public TownshipSchedulingProblemViewModel updateAndGet(
            TownshipSchedulingProblem townshipSchedulingProblem,
            TownshipSchedulingProblemViewModel townshipSchedulingProblemViewModel
    ) {
        this.setTownshipSchedulingProblem(townshipSchedulingProblem);
        SolverStatus solverStatus = getTownshipSchedulingProblem().getSolverStatus();
        HardMediumSoftBigDecimalScore score = getTownshipSchedulingProblem().getScore();
        return this.updateAndGet(
                townshipSchedulingProblemViewModel,
                solverStatus.name(),
                Objects.isNull(score)
                        ? "N/A"
                        : score.toString(),
                Objects.nonNull(score) && score.isFeasible()
        );
    }

    private TownshipSchedulingProblemViewModel updateAndGet(
            TownshipSchedulingProblemViewModel townshipSchedulingProblemViewModel,
            String solverStatus,
            String score,
            boolean feasible
    ) {
        if (townshipSchedulingProblemViewModel == null) {
            return mapAndGet();
        }
        return townshipSchedulingProblemViewModel.update(
                mapAndGetSchedulingProducingArrangementViewModel(),
                solverStatus,
                score,
                feasible
        );
    }

    public Collection<ReactiveSchedulingProducingArrangementViewModel> mapAndUpdateReactiveSchedulingProducingArrangementViewModel() {
        NavigableSet<SchedulingProducingArrangement> schedulingProducingArrangements = getTownshipSchedulingProblem().getSchedulingProducingArrangements();
        for (SchedulingProducingArrangement schedulingProducingArrangement : schedulingProducingArrangements) {
            mapAndUpdateSchedulingProducingArrangementViewModel(schedulingProducingArrangement);
        }
        return schedulingProducingArrangementToReactiveViewModelMap.values();
    }

    public Collection<SchedulingProducingArrangementViewModel> mapAndGetSchedulingProducingArrangementViewModel() {
        NavigableSet<SchedulingProducingArrangement> schedulingProducingArrangements = getTownshipSchedulingProblem().getSchedulingProducingArrangements();
        for (SchedulingProducingArrangement schedulingProducingArrangement : schedulingProducingArrangements) {
            mapAndGetSchedulingProducingArrangementViewModel(schedulingProducingArrangement);
        }
        return schedulingProducingArrangementToViewModelMap.values();
    }

    public ReactiveSchedulingProducingArrangementViewModel mapAndUpdateSchedulingProducingArrangementViewModel(SchedulingProducingArrangement schedulingProducingArrangement) {
        return schedulingProducingArrangementToReactiveViewModelMap.compute(
                schedulingProducingArrangement,
                (arrangementInMap, viewInMap) -> {
                    if (viewInMap == null) {
                        Integer id = schedulingProducingArrangement.getId();
                        String uuid = schedulingProducingArrangement.getUuid()
                                .toString();
                        ReactiveSchedulingProducingArrangementViewModel.ArrangementViewModelId modelId
                                = ReactiveSchedulingProducingArrangementViewModel.ArrangementViewModelId.of(
                                id,
                                uuid
                        );
                        SchedulingProductViewModel product = buildOrGetSchedulingProductViewModel(schedulingProducingArrangement.getSchedulingProduct());
                        SchedulingProductViewModel orderProduct = buildOrGetSchedulingProductViewModel(
                                schedulingProducingArrangement.getSchedulingOrderProduct()
                        );
                        SchedulingOrderViewModel order = buildOrGetSchedulingOrderViewModel(schedulingProducingArrangement.getSchedulingOrder());
                        ReactiveSchedulingProducingArrangementViewModel.ArrangementViewModelId orderProductArrangementId =
                                ReactiveSchedulingProducingArrangementViewModel.ArrangementViewModelId.of(
                                        schedulingProducingArrangement.getSupportOrderProducingArrangement()
                                                .getId(),
                                        schedulingProducingArrangement.getSupportOrderProducingArrangement()
                                                .getUuid()
                                                .toString()
                                );
                        boolean boolOrderDirect = schedulingProducingArrangement.boolOrderDirect();
                        Duration producingDuration = schedulingProducingArrangement.getProducingDuration();
                        Duration staticDeepPrerequisiteProducingDuration = schedulingProducingArrangement.getStaticDeepPrerequisiteProducingDuration();
                        Duration staticDeepProducingDuration = schedulingProducingArrangement.getStaticDeepProducingDuration();
                        SchedulingFactoryInstanceViewModel assignedFactoryInstance = buildOrGetSchedulingFactoryInstanceViewModel(
                                schedulingProducingArrangement.getPlanningFactoryInstance()
                        );
                        LocalDateTime arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
                        LocalDateTime producingDateTime = schedulingProducingArrangement.getProducingDateTime();
                        LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                        List<ReactiveSchedulingProducingArrangementViewModel.ArrangementViewModelId> prerequisiteProducingArrangements
                                = schedulingProducingArrangement.getPrerequisiteProducingArrangements()
                                .stream()
                                .map(this::mapAndUpdateSchedulingProducingArrangementViewModel)
                                .map(ReactiveSchedulingProducingArrangementViewModel::arrangementViewModelId)
                                .toList();
                        return new ReactiveSchedulingProducingArrangementViewModel(
                                modelId,
                                product,
                                orderProduct,
                                order,
                                orderProductArrangementId,
                                boolOrderDirect,
                                prerequisiteProducingArrangements,
                                producingDuration,
                                staticDeepPrerequisiteProducingDuration,
                                staticDeepProducingDuration,
                                new ValueSignal<>(assignedFactoryInstance),
                                new ValueSignal<>(arrangeDateTime),
                                new ValueSignal<>(producingDateTime),
                                new ValueSignal<>(completedDateTime)
                        );
                    } else {
                        viewInMap.assignedFactoryInstance().set(buildOrGetSchedulingFactoryInstanceViewModel(schedulingProducingArrangement.getPlanningFactoryInstance()));
                        viewInMap.arrangeDateTime().set(schedulingProducingArrangement.getArrangeDateTime());
                        viewInMap.producingDateTime().set(schedulingProducingArrangement.getProducingDateTime());
                        viewInMap.completedDateTime().set(schedulingProducingArrangement.getCompletedDateTime());
                        return viewInMap;
                    }
                }
        );
    }

    public SchedulingProducingArrangementViewModel mapAndGetSchedulingProducingArrangementViewModel(SchedulingProducingArrangement schedulingProducingArrangement) {
        return schedulingProducingArrangementToViewModelMap.compute(
                schedulingProducingArrangement,
                (arrangementInMap, viewInMap) -> {
                    if (viewInMap == null) {
                        Integer id = schedulingProducingArrangement.getId();
                        String uuid = schedulingProducingArrangement.getUuid()
                                .toString();
                        SchedulingProducingArrangementViewModel.SchedulingProducingArrangementViewModelId modelId
                                = SchedulingProducingArrangementViewModel.SchedulingProducingArrangementViewModelId.of(
                                id,
                                uuid
                        );
                        SchedulingProductViewModel product = buildOrGetSchedulingProductViewModel(schedulingProducingArrangement.getSchedulingProduct());
                        SchedulingProductViewModel orderProduct = buildOrGetSchedulingProductViewModel(
                                schedulingProducingArrangement.getSchedulingOrderProduct()
                        );
                        SchedulingOrderViewModel order = buildOrGetSchedulingOrderViewModel(schedulingProducingArrangement.getSchedulingOrder());
                        SchedulingProducingArrangementViewModel.SchedulingProducingArrangementViewModelId orderProductArrangementId =
                                SchedulingProducingArrangementViewModel.SchedulingProducingArrangementViewModelId.of(
                                        schedulingProducingArrangement.getSupportOrderProducingArrangement()
                                                .getId(),
                                        schedulingProducingArrangement.getSupportOrderProducingArrangement()
                                                .getUuid()
                                                .toString()
                                );
                        boolean boolOrderDirect = schedulingProducingArrangement.boolOrderDirect();
                        Duration producingDuration = schedulingProducingArrangement.getProducingDuration();
                        Duration staticDeepPrerequisiteProducingDuration = schedulingProducingArrangement.getStaticDeepPrerequisiteProducingDuration();
                        Duration staticDeepProducingDuration = schedulingProducingArrangement.getStaticDeepProducingDuration();
                        SchedulingFactoryInstanceViewModel assignedFactoryInstance = buildOrGetSchedulingFactoryInstanceViewModel(
                                schedulingProducingArrangement.getPlanningFactoryInstance()
                        );
                        LocalDateTime arrangeDateTime = schedulingProducingArrangement.getArrangeDateTime();
                        LocalDateTime producingDateTime = schedulingProducingArrangement.getProducingDateTime();
                        LocalDateTime completedDateTime = schedulingProducingArrangement.getCompletedDateTime();
                        List<SchedulingProducingArrangementViewModel.SchedulingProducingArrangementViewModelId> prerequisiteProducingArrangements
                                = schedulingProducingArrangement.getPrerequisiteProducingArrangements()
                                .stream()
                                .map(this::mapAndGetSchedulingProducingArrangementViewModel)
                                .map(SchedulingProducingArrangementViewModel::arrangementViewModelId)
                                .toList();
                        return new SchedulingProducingArrangementViewModel(
                                modelId,
                                product,
                                orderProduct,
                                order,
                                orderProductArrangementId,
                                boolOrderDirect,
                                prerequisiteProducingArrangements,
                                producingDuration,
                                staticDeepPrerequisiteProducingDuration,
                                staticDeepProducingDuration,
                                assignedFactoryInstance,
                                arrangeDateTime,
                                producingDateTime,
                                completedDateTime
                        );
                    } else {
                        return viewInMap.update(
                                buildOrGetSchedulingFactoryInstanceViewModel(schedulingProducingArrangement.getPlanningFactoryInstance()),
                                schedulingProducingArrangement.getArrangeDateTime(),
                                schedulingProducingArrangement.getProducingDateTime(),
                                schedulingProducingArrangement.getCompletedDateTime()
                        );
                    }
                }
        );
    }

    public Collection<SchedulingProductViewModel> mapAndGetSchedulingProductViewModel() {
        List<SchedulingProduct> schedulingProductList = getTownshipSchedulingProblem().getSchedulingProductList();
        for (SchedulingProduct schedulingProduct : schedulingProductList) {
            buildOrGetSchedulingProductViewModel(schedulingProduct);
        }
        return schedulingProductToViewModelMap.values();
    }

    private SchedulingProductViewModel buildOrGetSchedulingProductViewModel(SchedulingProduct schedulingProduct) {
        return schedulingProductToViewModelMap.computeIfAbsent(
                schedulingProduct,
                productInMap -> {
                    return new SchedulingProductViewModel(
                            SchedulingProductViewModel.SchedulingProductViewModelId.of(schedulingProduct.getId()
                                    .getValue()),
                            schedulingProduct.getName(),
                            schedulingProduct.getLevel(),
                            schedulingProduct.getGainWhenCompleted()
                    );
                }
        );
    }

    public Collection<SchedulingFactoryInfoViewModel> mapAndGetSchedulingFactoryInfoViewModel() {
        List<SchedulingFactoryInfo> schedulingFactoryInfoList = getTownshipSchedulingProblem().getSchedulingFactoryInfoList();
        for (SchedulingFactoryInfo schedulingFactoryInfo : schedulingFactoryInfoList) {
            buildOrGetSchedulingFactoryInfoViewModel(schedulingFactoryInfo);
        }
        return schedulingFactoryInfoToViewModelMap.values();
    }

    private SchedulingFactoryInfoViewModel buildOrGetSchedulingFactoryInfoViewModel(SchedulingFactoryInfo schedulingFactoryInfo) {
        return this.schedulingFactoryInfoToViewModelMap.computeIfAbsent(
                schedulingFactoryInfo,
                infoViewInMap -> {
                    List<SchedulingProductViewModel> schedulingProductViewModelList = schedulingFactoryInfo.getPortfolio()
                            .stream()
                            .map(this::buildOrGetSchedulingProductViewModel)
                            .toList();
                    return new SchedulingFactoryInfoViewModel(
                            schedulingFactoryInfo.getId()
                                    .getValue(),
                            schedulingFactoryInfo.getCategoryName(),
                            schedulingFactoryInfo.getLevel(),
                            schedulingFactoryInfo.getProducingStructureType()
                                    .name(),
                            schedulingProductViewModelList,
                            schedulingFactoryInfo.getDefaultInstanceAmount(),
                            schedulingFactoryInfo.getDefaultProducingCapacity(),
                            schedulingFactoryInfo.getDefaultReapWindowCapacity(),
                            schedulingFactoryInfo.getMaxProducingCapacity(),
                            schedulingFactoryInfo.getMaxReapWindowCapacity(),
                            schedulingFactoryInfo.getMaxInstanceAmount()
                    );
                }
        );
    }

    public Collection<SchedulingFactoryInstanceViewModel> mapAndGetSchedulingFactoryInstanceViewModel() {
        List<SchedulingFactoryInstance> schedulingFactoryInstanceList = getTownshipSchedulingProblem().getSchedulingFactoryInstanceList();
        for (SchedulingFactoryInstance schedulingFactoryInstance : schedulingFactoryInstanceList) {
            buildOrGetSchedulingFactoryInstanceViewModel(schedulingFactoryInstance);
        }
        return schedulingFactoryInstanceToViewModelMap.values();
    }

    private SchedulingFactoryInstanceViewModel buildOrGetSchedulingFactoryInstanceViewModel(SchedulingFactoryInstance planningFactoryInstance) {
        if (planningFactoryInstance == null) {
            return null;
        }

        return schedulingFactoryInstanceToViewModelMap.computeIfAbsent(
                planningFactoryInstance,
                factoryInMap -> new SchedulingFactoryInstanceViewModel(
                        planningFactoryInstance.getId(),
                        planningFactoryInstance.getFieldFactoryId(),
                        planningFactoryInstance.getSchedulingFactoryInfo()
                                .getLevel(),
                        planningFactoryInstance.getCategoryName(),
                        planningFactoryInstance.getSeqNum(),
                        planningFactoryInstance.getProducingLength(),
                        planningFactoryInstance.getReapWindowSize(),
                        planningFactoryInstance.getFactoryReadableIdentifier()
                                .toString()
                )
        );
    }

    public Collection<SchedulingOrderViewModel> mapAndGetSchedulingOrderViewModel() {
        List<SchedulingOrder> schedulingOrderList = getTownshipSchedulingProblem().getSchedulingOrderList();
        for (SchedulingOrder schedulingOrder : schedulingOrderList) {
            buildOrGetSchedulingOrderViewModel(schedulingOrder);
        }
        return this.schedulingOrderToViewModelMap.values();
    }

    private SchedulingOrderViewModel buildOrGetSchedulingOrderViewModel(SchedulingOrder schedulingOrder) {
        ProductAmountBill productAmountBill = schedulingOrder.getProductAmountBill();
        List<SchedulingProductAmountPair> productAmountPairs = new ArrayList<>();
        SchedulingProductAmountPair schedulingProductAmountPair;
        for (Map.Entry<SchedulingProduct, Integer> productIntegerEntry : productAmountBill.entrySet()) {
            SchedulingProduct product = productIntegerEntry.getKey();
            Integer amount = productIntegerEntry.getValue();
            schedulingProductAmountPair = new SchedulingProductAmountPair(
                    buildOrGetSchedulingProductViewModel(product),
                    amount
            );
            productAmountPairs.add(schedulingProductAmountPair);
        }
        return schedulingOrderToViewModelMap.computeIfAbsent(
                schedulingOrder,
                orderInMap -> {
                    return new SchedulingOrderViewModel(
                            schedulingOrder.getId(),
                            schedulingOrder.getOrderType()
                                    .name(),
                            schedulingOrder.getDeadline(),
                            new ProductAmountBillViewModel(productAmountPairs)
                    );
                }
        );
    }

    public Collection<SchedulingDateTimeSlotViewModel> mapAndGetSchedulingDateTimeSlotViewModel() {
        NavigableSet<SchedulingDateTimeSlot> schedulingDateTimeSlots = getTownshipSchedulingProblem().getSchedulingDateTimeSlots();
        for (SchedulingDateTimeSlot schedulingDateTimeSlot : schedulingDateTimeSlots) {
            buildOrGetSchedulingDateTimeSlotViewModel(schedulingDateTimeSlot);
        }
        return schedulingDateTimeSlotToViewModelMap.values();
    }

    private SchedulingDateTimeSlotViewModel buildOrGetSchedulingDateTimeSlotViewModel(SchedulingDateTimeSlot schedulingDateTimeSlot) {
        return this.schedulingDateTimeSlotToViewModelMap.computeIfAbsent(
                schedulingDateTimeSlot,
                slotViewInMap -> {
                    return new SchedulingDateTimeSlotViewModel(
                            schedulingDateTimeSlot.getId(),
                            schedulingDateTimeSlot.getStart(),
                            schedulingDateTimeSlot.getEnd()
                    );
                }
        );
    }

    public SchedulingWorkCalendarViewModel mapAndGetSchedulingWorkCalendarViewModel() {
        SchedulingWorkCalendar schedulingWorkCalendar = getTownshipSchedulingProblem().getSchedulingWorkCalendar();
        return new SchedulingWorkCalendarViewModel(
                schedulingWorkCalendar.getStartDateTime(),
                schedulingWorkCalendar.getEndDateTime()
        );
    }

    public SchedulingPlayerViewModel mapAndGetSchedulingPlayerViewModel() {
        SchedulingPlayer schedulingPlayer = getTownshipSchedulingProblem().getSchedulingPlayer();
        return new SchedulingPlayerViewModel(
                schedulingPlayer.getId(),
                schedulingPlayer.getSleepStart(),
                schedulingPlayer.getSleepEnd()
        );
    }

}
