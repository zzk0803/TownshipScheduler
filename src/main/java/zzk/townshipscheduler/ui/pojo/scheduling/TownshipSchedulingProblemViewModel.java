package zzk.townshipscheduler.ui.pojo.scheduling;

import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DTO for {@link TownshipSchedulingProblem}
 */
public record TownshipSchedulingProblemViewModel(
        String uuid,
        Collection<SchedulingProductViewModel> schedulingProductViewModels,
        Collection<SchedulingFactoryInfoViewModel> schedulingFactoryInfoViewModels,
        Collection<SchedulingOrderViewModel> schedulingOrderViewModels,
        Collection<SchedulingFactoryInstanceViewModel> schedulingFactoryInstanceViewModels,
        Collection<SchedulingDateTimeSlotViewModel> schedulingDateTimeSlotViewModels,
        Collection<SchedulingProducingArrangementViewModel> schedulingProducingArrangementViewModels,
        SchedulingWorkCalendarViewModel schedulingWorkCalendar,
        int dateTimeSlotDurationInMinute,
        SchedulingPlayerViewModel schedulingPlayer,
        String solverStatus,
        String score,
        boolean feasible
)
        implements Serializable {

    public static final TownshipSchedulingProblemViewModel EMPTY_NULL_VALUE
            = new TownshipSchedulingProblemViewModel(
            "0",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            null,
            0,
            null,
            "N/A",
            "N/A",
            false
    );

    public TownshipSchedulingProblemViewModel update(
            Collection<SchedulingProducingArrangementViewModel> schedulingProducingArrangements,
            String solverStatus,
            String score,
            boolean feasible
    ) {
        return new TownshipSchedulingProblemViewModel(
                this.uuid,
                this.schedulingProductViewModels,
                this.schedulingFactoryInfoViewModels,
                this.schedulingOrderViewModels,
                this.schedulingFactoryInstanceViewModels,
                this.schedulingDateTimeSlotViewModels,
                schedulingProducingArrangements,
                this.schedulingWorkCalendar,
                dateTimeSlotDurationInMinute,
                this.schedulingPlayer,
                solverStatus,
                score,
                feasible
        );
    }

    public SchedulingReportGroupsViewModel toSchedulingReportGroupsViewModel() {
        ArrayList<SchedulingReportArrangeDateTimeGroupViewModel> schedulingReportArrangeDateTimeGroupViewModels = schedulingProducingArrangementViewModels.stream()
                .filter(schedulingProducingArrangement -> Objects.nonNull(schedulingProducingArrangement.arrangeDateTime()))
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(
                                        SchedulingProducingArrangementViewModel::arrangeDateTime,
                                        TreeMap::new,
                                        Collectors.collectingAndThen(
                                                Collectors.groupingBy(
                                                        SchedulingProducingArrangementViewModel::assignedFactoryInstance,
                                                        Collectors.collectingAndThen(
                                                                Collectors.groupingBy(
                                                                        SchedulingProducingArrangementViewModel::product,
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
                                                schedulingFactoryInstanceViewModelProductAmountBillViewModelMap -> {
                                                    return schedulingFactoryInstanceViewModelProductAmountBillViewModelMap.entrySet()
                                                            .stream()
                                                            .map(schedulingFactoryInstanceViewModelProductAmountBillViewModelEntry -> {
                                                                return new SchedulingReportFactoryGroupViewModel(
                                                                        schedulingFactoryInstanceViewModelProductAmountBillViewModelEntry.getKey(),
                                                                        schedulingFactoryInstanceViewModelProductAmountBillViewModelEntry.getValue()
                                                                );
                                                            })
                                                            .collect(Collectors.toCollection(ArrayList::new));
                                                }
                                        )
                                ),
                                localDateTimeCollectionTreeMap -> {
                                    return localDateTimeCollectionTreeMap.entrySet().stream().map(localDateTimeCollectionEntry -> {
                                        LocalDateTime arrangeDateTime = localDateTimeCollectionEntry.getKey();
                                        Collection<SchedulingReportFactoryGroupViewModel> localDateTimeCollectionEntryValue = localDateTimeCollectionEntry.getValue();
                                        return new SchedulingReportArrangeDateTimeGroupViewModel(
                                                arrangeDateTime,
                                                localDateTimeCollectionEntryValue
                                        );
                                    });
                                }
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

    public List<LitSchedulingProducingArrangementVO> toLitSchedulingProducingArrangementVoList() {
        return schedulingProducingArrangementViewModels.stream()
                .map(LitSchedulingProducingArrangementVO::of)
                .toList();
    }

    public List<LitSchedulingProducingArrangementUnitGroupViewModel> toProducingArrangementUnitGroupVoList() {
        Map<SchedulingOrderViewModel, List<SchedulingProducingArrangementViewModel>> orderArrangeMap
                = schedulingProducingArrangementViewModels.stream()
                .filter(SchedulingProducingArrangementViewModel::boolDirectToOrder)
                .collect(Collectors.groupingBy(SchedulingProducingArrangementViewModel::order));

        return orderArrangeMap.entrySet()
                .stream()
                .map(
                        orderAndArrangeList -> {
                            SchedulingOrderViewModel schedulingOrder = orderAndArrangeList.getKey();
                            List<SchedulingProducingArrangementViewModel> arrangeListValue = orderAndArrangeList.getValue();
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
