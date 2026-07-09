package zzk.townshipscheduler.ui.pojo.scheduling.reactive;

import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import zzk.townshipscheduler.ui.pojo.scheduling.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public record ReactiveTownshipSchedulingProblemViewModel(
        String uuid,
        Collection<SchedulingProductViewModel> schedulingProductViewModels,
        Collection<SchedulingFactoryInfoViewModel> schedulingFactoryInfoViewModels,
        Collection<SchedulingOrderViewModel> schedulingOrderViewModels,
        Collection<SchedulingFactoryInstanceViewModel> schedulingFactoryInstanceViewModels,
        Collection<SchedulingDateTimeSlotViewModel> schedulingDateTimeSlotViewModels,
        ListSignal<ReactiveSchedulingProducingArrangementViewModel> schedulingProducingArrangementReactiveViewModels,
        SchedulingWorkCalendarViewModel schedulingWorkCalendar,
        int dateTimeSlotDurationInMinute,
        SchedulingPlayerViewModel schedulingPlayer,
        ValueSignal<String> solverStatus,
        ValueSignal<String> score,
        ValueSignal<Boolean> feasible
)
        implements Serializable {

    public static final ReactiveTownshipSchedulingProblemViewModel EMPTY_NULL_VALUE
            = new ReactiveTownshipSchedulingProblemViewModel(
            "0",
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            new ListSignal<>(),
            null,
            0,
            null,
            new ValueSignal<>("N/A"),
            new ValueSignal<>("N/A"),
            new ValueSignal<>(false)
    );

    public Collection<ReactiveTownshipSchedulingProblemOrderBriefViewModel> toTownshipSchedulingProblemOrderBriefViewModels() {
        return schedulingOrderViewModels.stream()
                .map(
                        schedulingOrder -> {
                            LocalDateTime deadline = schedulingOrder.deadline() != null
                                    ? schedulingOrder.deadline()
                                    : schedulingWorkCalendar.endDateTime();
                            List<ReactiveSchedulingProducingArrangementViewModel> list
                                    = this.schedulingProducingArrangementReactiveViewModels.getValues()
                                    .filter(reactiveSchedulingProducingArrangementViewModel -> reactiveSchedulingProducingArrangementViewModel.order()
                                            .equals(schedulingOrder))
                                    .toList();
                            return new ReactiveTownshipSchedulingProblemOrderBriefViewModel(
                                    Math.toIntExact(schedulingOrder.id()),
                                    schedulingOrder.orderType(),
                                    deadline,
                                    schedulingOrder.productAmountBill(),
                                    list
                            );
                        })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public SchedulingReportGroupsViewModel toSchedulingReportGroupsViewModel() {
        ArrayList<SchedulingReportArrangeDateTimeGroupViewModel> schedulingReportArrangeDateTimeGroupViewModels = schedulingProducingArrangementReactiveViewModels.getValues()
                .filter(schedulingProducingArrangement -> Objects.nonNull(schedulingProducingArrangement.arrangeDateTime()))
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(
                                        reactiveSchedulingProducingArrangementViewModel -> reactiveSchedulingProducingArrangementViewModel.arrangeDateTime().get(),
                                        TreeMap::new,
                                        Collectors.collectingAndThen(
                                                Collectors.groupingBy(
                                                        reactiveSchedulingProducingArrangementViewModel1 -> reactiveSchedulingProducingArrangementViewModel1.assignedFactoryInstance().get(),
                                                        Collectors.collectingAndThen(
                                                                Collectors.groupingBy(
                                                                        ReactiveSchedulingProducingArrangementViewModel::product,
                                                                        Collectors.counting()
                                                                ),
                                                                schedulingProductViewModelLongMap -> {
                                                                    Collection<SchedulingProductAmountPair> schedulingProductAmountPairs
                                                                            = schedulingProductViewModelLongMap.entrySet()
                                                                            .stream()
                                                                            .map(
                                                                                    schedulingProductViewModelLongEntry -> {
                                                                                        return new SchedulingProductAmountPair(
                                                                                                schedulingProductViewModelLongEntry.getKey(),
                                                                                                Math.toIntExact(schedulingProductViewModelLongEntry.getValue())
                                                                                        );
                                                                                    })
                                                                            .collect(Collectors.toCollection(ArrayList::new));
                                                                    return new ProductAmountBillViewModel(schedulingProductAmountPairs);
                                                                }
                                                        )
                                                ),
                                                schedulingFactoryInstanceViewModelProductAmountBillViewModelMap -> schedulingFactoryInstanceViewModelProductAmountBillViewModelMap.entrySet()
                                                        .stream()
                                                        .map(schedulingFactoryInstanceViewModelProductAmountBillViewModelEntry -> {
                                                            return new SchedulingReportFactoryGroupViewModel(
                                                                    schedulingFactoryInstanceViewModelProductAmountBillViewModelEntry.getKey(),
                                                                    schedulingFactoryInstanceViewModelProductAmountBillViewModelEntry.getValue()
                                                            );
                                                        })
                                                        .collect(Collectors.toCollection(ArrayList::new))
                                        )
                                ),
                                localDateTimeCollectionTreeMap -> localDateTimeCollectionTreeMap.entrySet().stream().map(localDateTimeCollectionEntry -> {
                                    LocalDateTime arrangeDateTime = localDateTimeCollectionEntry.getKey();
                                    Collection<SchedulingReportFactoryGroupViewModel> localDateTimeCollectionEntryValue = localDateTimeCollectionEntry.getValue();
                                    return new SchedulingReportArrangeDateTimeGroupViewModel(
                                            arrangeDateTime,
                                            localDateTimeCollectionEntryValue
                                    );
                                })
                        )
                ).collect(Collectors.toCollection(ArrayList::new));
        return new SchedulingReportGroupsViewModel(schedulingReportArrangeDateTimeGroupViewModels);
    }

    public List<LitSchedulingOrderVo> toLitOrderVoList() {
        return schedulingOrderViewModels.stream()
                .map(SchedulingOrderViewModel::toLitSchedulingOrderVo)
                .toList();
    }

    public List<LitSchedulingFactoryInstanceVO> toLitFactoryInstanceVoList() {
        return schedulingFactoryInstanceViewModels.stream()
                .sorted(
                        Comparator.comparing(
                                SchedulingFactoryInstanceViewModel::level
                        )
                )
                .map(schedulingFactoryInstance -> {
                    Integer id = schedulingFactoryInstance.id();
                    String categoryName = schedulingFactoryInstance.categoryName();
                    int seqNum = schedulingFactoryInstance.seqNum();
                    int producingLength = schedulingFactoryInstance.producingLength();
                    int reapWindowSize = schedulingFactoryInstance.reapWindowSize();
                    String factoryReadableIdentifier = schedulingFactoryInstance.factoryReadableIdentifier();

                    return new LitSchedulingFactoryInstanceVO(
                            id,
                            categoryName,
                            seqNum,
                            producingLength,
                            reapWindowSize,
                            factoryReadableIdentifier
                    );
                })
                .toList();
    }

    public List<LitSchedulingProducingArrangementUnitGroupViewModel> toProducingArrangementUnitGroupVoList() {
        Map<SchedulingOrderViewModel, List<ReactiveSchedulingProducingArrangementViewModel>> orderArrangeMap
                = schedulingProducingArrangementReactiveViewModels.peekValues()
                .filter(ReactiveSchedulingProducingArrangementViewModel::boolDirectToOrder)
                .collect(Collectors.groupingBy(ReactiveSchedulingProducingArrangementViewModel::order));

        return orderArrangeMap.entrySet()
                .stream()
                .map(
                        orderAndArrangeList -> {
                            SchedulingOrderViewModel schedulingOrder = orderAndArrangeList.getKey();
                            List<ReactiveSchedulingProducingArrangementViewModel> arrangeListValue = orderAndArrangeList.getValue();
                            Set<LitSchedulingProducingArrangementUnitGroupViewModel.NestedOrderProductViewModel> nestedOrderProductViewModelSet
                                    = arrangeListValue.stream()
                                    .map(schedulingProducingArrangement -> {
                                        SchedulingProductViewModel schedulingProductViewModel = schedulingProducingArrangement.orderProduct();

                                        return LitSchedulingProducingArrangementUnitGroupViewModel.NestedOrderProductViewModel.of(
                                                schedulingProductViewModel.name(),
                                                schedulingProducingArrangement.orderProductArrangementId()
                                                        .id()
                                        );
                                    })
                                    .collect(Collectors.toCollection(HashSet::new));
                            LitSchedulingProducingArrangementUnitGroupViewModel groupVo
                                    = new LitSchedulingProducingArrangementUnitGroupViewModel(
                                    schedulingOrder.id(),
                                    schedulingOrder.orderType(),
                                    nestedOrderProductViewModelSet
                            );

                            groupVo.addAll(nestedOrderProductViewModelSet);
                            return groupVo;
                        }
                )
                .toList();
    }

}
