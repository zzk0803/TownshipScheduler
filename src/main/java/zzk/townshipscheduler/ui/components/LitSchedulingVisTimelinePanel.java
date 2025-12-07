package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.ui.pojo.LitSchedulingOrderVo;
import zzk.townshipscheduler.ui.pojo.SchedulingFactoryInstanceVO;
import zzk.townshipscheduler.ui.pojo.SchedulingProducingArrangementUnitGroupVo;
import zzk.townshipscheduler.ui.pojo.SchedulingProducingArrangementVO;
import zzk.townshipscheduler.ui.views.scheduling.SchedulingViewPresenter;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Tag("scheduling-vis-timeline-panel")
@NpmPackage(value = "vis-timeline", version = "8.4.1")
@NpmPackage(value = "@js-joda/core", version = "5.6.5")
@JsModule("./src/components/scheduling-vis-timeline-panel.ts")
@JsModule("./src/components/by-factory-timeline-components.ts")
@JsModule("./src/components/by-order-timeline-components.ts")
@JsModule("./src/components/by-unit-timeline-components.ts")
@JsModule("./src/components/lit-vis-timeline.ts")
public class LitSchedulingVisTimelinePanel extends Component {

    private SchedulingViewPresenter schedulingViewPresenter;

    public LitSchedulingVisTimelinePanel(SchedulingViewPresenter schedulingViewPresenter) {
        this.schedulingViewPresenter = schedulingViewPresenter;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        pullScheduleResult();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
    }

    @ClientCallable
    public void pullScheduleResult() {
        updateRemoteFull();
    }

    public void updateRemoteFull() {
        updateRemoteFull(this.schedulingViewPresenter.getTownshipSchedulingProblem());
    }

    private void updateRemoteFull(TownshipSchedulingProblem townshipSchedulingProblem) {
        setPropertyObject("schedulingWorkCalendar", townshipSchedulingProblem.getSchedulingWorkCalendar());
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
                toProducingArrangementVo(townshipSchedulingProblem.getSchedulingProducingArrangementList())
        );
        setPropertyList(
                "schedulingProducingArrangementUnitGroups",
                toProducingArrangementUnitGroupVo(townshipSchedulingProblem.getSchedulingProducingArrangementList())
        );
        setPropertyNumber(
                "dateTimeSlotSizeInMinute",
                townshipSchedulingProblem.getDateTimeSlotSize().getMinute()
        );

    }

    private void setPropertyList(String name, List<?> listObject) {
        getElement().setPropertyList(name, listObject);
    }

    private List<LitSchedulingOrderVo> toOrderVo(List<SchedulingOrder> schedulingOrderList) {
        return schedulingOrderList.stream()
                .map(schedulingOrder -> new LitSchedulingOrderVo(
                        schedulingOrder.getId(),
                        schedulingOrder.getOrderType().name(),
                        Optional.ofNullable(schedulingOrder.getDeadline())
                                .map(localDateTime -> localDateTime.format(
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                )
                                .orElse("N/A")
                ))
                .toList();
    }

    private List<SchedulingFactoryInstanceVO> toFactoryInstanceVo(
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
                    int producingLength = schedulingFactoryInstance.getProducingQueue();
                    int reapWindowSize = schedulingFactoryInstance.getReapWindowSize();
                    FactoryReadableIdentifier factoryReadableIdentifier
                            = schedulingFactoryInstance.getFactoryReadableIdentifier();

                    return new SchedulingFactoryInstanceVO(
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

    private List<SchedulingProducingArrangementVO> toProducingArrangementVo(
            List<SchedulingProducingArrangement> schedulingProducingArrangementList
    ) {
        return schedulingProducingArrangementList.stream()
                .map(SchedulingProducingArrangementVO::new)
                .toList();

    }

    private List<SchedulingProducingArrangementUnitGroupVo> toProducingArrangementUnitGroupVo(
            List<SchedulingProducingArrangement> schedulingProducingArrangementList
    ) {
        Map<SchedulingOrder, List<SchedulingProducingArrangement>> orderArrangeMap
                = schedulingProducingArrangementList.stream()
                .filter(SchedulingProducingArrangement::isOrderDirect)
                .collect(
                        Collectors.groupingBy(SchedulingProducingArrangement::getSchedulingOrder)
                );

        return orderArrangeMap.entrySet().stream()
                .map(
                        orderAndArrangeList -> {
                            SchedulingOrder schedulingOrder = orderAndArrangeList.getKey();
                            List<SchedulingProducingArrangement> arrangeListValue = orderAndArrangeList.getValue();
                            SchedulingProducingArrangementUnitGroupVo groupVo
                                    = new SchedulingProducingArrangementUnitGroupVo(
                                    schedulingOrder.getId(),
                                    schedulingOrder.getOrderType().name()
                            );

                            Set<SchedulingProducingArrangementUnitGroupVo.NestedOrderProduct> nestedOrderProductSet = arrangeListValue.stream()
                                    .map(schedulingProducingArrangement -> {
                                        SchedulingProduct schedulingOrderProduct = schedulingProducingArrangement.getSchedulingOrderProduct();

                                        return SchedulingProducingArrangementUnitGroupVo.NestedOrderProduct.of(
                                                schedulingOrderProduct.getName(),
                                                schedulingProducingArrangement.getId()
                                        );
                                    })
                                    .collect(Collectors.toSet());

                            groupVo.addAll(nestedOrderProductSet);
                            return groupVo;
                        }
                )
                .toList();
    }

    private void setPropertyNumber(String name, double value) {
        getElement().setProperty(name, value);
    }

    private void setPropertyObject(String name, Object object) {
        getElement().setPropertyBean(name, object);
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
                        townshipSchedulingProblem.getSchedulingProducingArrangementList()
                )
        );
    }

    private void setPropertyMap(String name, Map<String, ?> map) {
        getElement().setPropertyMap(name, map);
    }

}
