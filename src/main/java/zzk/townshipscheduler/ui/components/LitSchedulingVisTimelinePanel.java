package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.ui.pojo.LitSchedulingFactoryInstanceVO;
import zzk.townshipscheduler.ui.pojo.LitSchedulingOrderVo;
import zzk.townshipscheduler.ui.pojo.LitSchedulingProducingArrangementUnitGroupViewModel;
import zzk.townshipscheduler.ui.pojo.LitSchedulingProducingArrangementVO;
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
        updateRemoteFull(this.schedulingViewPresenter.getTownshipSchedulingProblem());
    }

    private void updateRemoteFull(TownshipSchedulingProblem townshipSchedulingProblem) {
        setPropertyObject(
                "schedulingWorkCalendar",
                townshipSchedulingProblem.getSchedulingWorkCalendar()
        );
        setPropertyList(
                "schedulingOrders",
                toOrderVo(townshipSchedulingProblem.getSchedulingOrderList())
        );
        setPropertyList(
                "schedulingProducts",
                townshipSchedulingProblem.getSchedulingProductList()
        );
        setPropertyList(
                "schedulingFactoryInstances",
                toFactoryInstanceVo(townshipSchedulingProblem.getSchedulingFactoryInstanceList())
        );
        setPropertyList(
                "schedulingProducingArrangements",
                toProducingArrangementVo(townshipSchedulingProblem.getSchedulingProducingArrangements())
        );
        setPropertyList(
                "schedulingProducingArrangementUnitGroups",
                toProducingArrangementUnitGroupVo(townshipSchedulingProblem.getSchedulingProducingArrangements())
        );
        setPropertyNumber(
                "dateTimeSlotSizeInMinute",
                townshipSchedulingProblem.getDateTimeSlotSize()
                        .getMinute()
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

    private List<LitSchedulingOrderVo> toOrderVo(List<SchedulingOrder> schedulingOrderList) {
        return schedulingOrderList.stream()
                .map(schedulingOrder -> new LitSchedulingOrderVo(
                        schedulingOrder.getId(),
                        schedulingOrder.getOrderType()
                                .name(),
                        Optional.ofNullable(schedulingOrder.getDeadline())
                                .map(localDateTime -> localDateTime.format(
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                )
                                .orElse("N/A")
                ))
                .toList();
    }

    private List<LitSchedulingFactoryInstanceVO> toFactoryInstanceVo(
            List<SchedulingFactoryInstance> schedulingFactoryInstanceList
    ) {
        return schedulingFactoryInstanceList.stream()
                .sorted(
                        Comparator.comparing(
                                schedulingFactoryInstance -> schedulingFactoryInstance.getSchedulingFactoryInfo()
                                        .getLevel())
                )
                .map(schedulingFactoryInstance -> {
                    Integer id = schedulingFactoryInstance.getId();
                    String categoryName = schedulingFactoryInstance.getCategoryName();
                    int seqNum = schedulingFactoryInstance.getSeqNum();
                    int producingLength = schedulingFactoryInstance.getProducingLength();
                    int reapWindowSize = schedulingFactoryInstance.getReapWindowSize();
                    FactoryReadableIdentifier factoryReadableIdentifier
                            = schedulingFactoryInstance.getFactoryReadableIdentifier();

                    return new LitSchedulingFactoryInstanceVO(
                            id,
                            categoryName,
                            seqNum,
                            producingLength,
                            reapWindowSize,
                            factoryReadableIdentifier.toString()
                    );
                })
                .toList();
    }

    private List<LitSchedulingProducingArrangementVO> toProducingArrangementVo(
            Collection<SchedulingProducingArrangement> schedulingProducingArrangementList
    ) {
        return schedulingProducingArrangementList.stream()
                .map(LitSchedulingProducingArrangementVO::new)
                .toList();

    }

    private List<LitSchedulingProducingArrangementUnitGroupViewModel> toProducingArrangementUnitGroupVo(
            Collection<SchedulingProducingArrangement> schedulingProducingArrangementList
    ) {
        Map<SchedulingOrder, List<SchedulingProducingArrangement>> orderArrangeMap
                = schedulingProducingArrangementList.stream()
                .filter(SchedulingProducingArrangement::boolOrderDirect)
                .collect(
                        Collectors.groupingBy(SchedulingProducingArrangement::getSchedulingOrder)
                );

        return orderArrangeMap.entrySet()
                .stream()
                .map(
                        orderAndArrangeList -> {
                            SchedulingOrder schedulingOrder = orderAndArrangeList.getKey();
                            List<SchedulingProducingArrangement> arrangeListValue = orderAndArrangeList.getValue();
                            Set<LitSchedulingProducingArrangementUnitGroupViewModel.NestedOrderProductViewModel> nestedOrderProductViewModelSet
                                    = arrangeListValue.stream()
                                    .map(schedulingProducingArrangement -> {
                                        SchedulingProduct schedulingOrderProduct = schedulingProducingArrangement.getSchedulingOrderProduct();

                                        return LitSchedulingProducingArrangementUnitGroupViewModel.NestedOrderProductViewModel.of(
                                                schedulingOrderProduct.getName(),
                                                schedulingProducingArrangement.getId()
                                        );
                                    })
                                    .collect(Collectors.toSet());
                            LitSchedulingProducingArrangementUnitGroupViewModel groupVo
                                    = new LitSchedulingProducingArrangementUnitGroupViewModel(
                                    schedulingOrder.getId(),
                                    schedulingOrder.getOrderType()
                                            .name(),
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
                this.schedulingViewPresenter.getTownshipSchedulingProblem()
        );
    }

    private void updateRemoteArrangements(
            TownshipSchedulingProblem townshipSchedulingProblem
    ) {
        setPropertyList(
                "schedulingProducingArrangements",
                toProducingArrangementVo(
                        townshipSchedulingProblem.getSchedulingProducingArrangements()
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
