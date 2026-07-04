package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import zzk.townshipscheduler.ui.pojo.*;
import zzk.townshipscheduler.ui.views.scheduling.SchedulingViewPresenter;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Tag("scheduling-vis-timeline-panel")
@NpmPackage(value = "vis-timeline", version = "8.5.1")
@NpmPackage(value = "@js-joda/core", version = "6.0.1")
@JsModule("./components/scheduling-vis-timeline-panel.ts")
@JsModule("./components/by-factory-timeline-components.ts")
@JsModule("./components/by-order-timeline-components.ts")
@JsModule("./components/by-unit-timeline-components.ts")
@JsModule("./components/lit-vis-timeline.ts")
public class LitSchedulingVisTimelinePanel extends Component {

    private final SchedulingViewPresenter schedulingViewPresenter;

    public LitSchedulingVisTimelinePanel(SchedulingViewPresenter schedulingViewPresenter) {
        this.schedulingViewPresenter = schedulingViewPresenter;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        pullScheduleResult();
    }

    @ClientCallable
    public void pullScheduleResult() {
        updateRemoteFull();
    }

    public void updateRemoteFull() {
        updateRemoteFull(this.schedulingViewPresenter.getTownshipSchedulingProblemViewModel());
    }

    private void updateRemoteFull(TownshipSchedulingProblemViewModel townshipSchedulingProblem) {
        setPropertyObject(
                "schedulingWorkCalendar",
                townshipSchedulingProblem.schedulingWorkCalendar()
        );
        setPropertyList(
                "schedulingOrders",
                toOrderVo(townshipSchedulingProblem.schedulingOrderViewModels())
        );
        setPropertyList(
                "schedulingProducts",
                Arrays.asList(townshipSchedulingProblem.schedulingProductViewModels()
                        .toArray())
        );
        setPropertyList(
                "schedulingFactoryInstances",
                toFactoryInstanceVo(townshipSchedulingProblem.schedulingFactoryInstanceViewModels())
        );
        setPropertyList(
                "schedulingProducingArrangements",
                toProducingArrangementVo(townshipSchedulingProblem.schedulingProducingArrangementViewModels())
        );
        setPropertyList(
                "schedulingProducingArrangementUnitGroups",
                toProducingArrangementUnitGroupVo(townshipSchedulingProblem.schedulingProducingArrangementViewModels())
        );
        setPropertyNumber(
                "dateTimeSlotSizeInMinute",
                townshipSchedulingProblem.dateTimeSlotDurationInMinute()
        );

    }

    private void setPropertyObject(
            String name,
            Object object
    ) {
        getElement().setPropertyBean(
                name,
                object
        );
    }

    private void setPropertyList(
            String name,
            List<?> listObject
    ) {
        getElement().setPropertyList(
                name,
                listObject
        );
    }

    private List<LitSchedulingOrderVo> toOrderVo(Collection<SchedulingOrderViewModel> schedulingOrderList) {
        return schedulingOrderList.stream()
                .map(schedulingOrder -> new LitSchedulingOrderVo(
                        schedulingOrder.id(),
                        schedulingOrder.orderType(),
                        Optional.ofNullable(schedulingOrder.deadline())
                                .map(localDateTime -> localDateTime.format(
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                )
                                .orElse("N/A")
                ))
                .toList();
    }

    private List<LitSchedulingFactoryInstanceVO> toFactoryInstanceVo(
            Collection<SchedulingFactoryInstanceViewModel> schedulingFactoryInstanceList
    ) {
        return schedulingFactoryInstanceList.stream()
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

    private List<LitSchedulingProducingArrangementVO> toProducingArrangementVo(
            Collection<SchedulingProducingArrangementViewModel> schedulingProducingArrangementList
    ) {
        return schedulingProducingArrangementList.stream()
                .map(LitSchedulingProducingArrangementVO::new)
                .toList();

    }

    private List<LitSchedulingProducingArrangementUnitGroupViewModel> toProducingArrangementUnitGroupVo(
            Collection<SchedulingProducingArrangementViewModel> schedulingProducingArrangementList
    ) {
        Map<SchedulingOrderViewModel, List<SchedulingProducingArrangementViewModel>> orderArrangeMap
                = schedulingProducingArrangementList.stream()
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

    private void setPropertyNumber(
            String name,
            double value
    ) {
        getElement().setProperty(
                name,
                value
        );
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
    }

    public void updateRemoteArrangements() {
        updateRemoteArrangements(
                this.schedulingViewPresenter.getTownshipSchedulingProblemViewModel()
        );
    }

    private void updateRemoteArrangements(
            TownshipSchedulingProblemViewModel townshipSchedulingProblem
    ) {
        setPropertyList(
                "schedulingProducingArrangements",
                toProducingArrangementVo(
                        townshipSchedulingProblem.schedulingProducingArrangementViewModels()
                )
        );
    }

    private void setPropertyMap(
            String name,
            Map<String, ?> map
    ) {
        getElement().setPropertyMap(
                name,
                map
        );
    }

}
