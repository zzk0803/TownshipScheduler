package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.ui.pojo.LitSchedulingOrderVo;
import zzk.townshipscheduler.ui.pojo.SchedulingFactoryInstanceVO;
import zzk.townshipscheduler.ui.pojo.SchedulingProducingArrangementVO;
import zzk.townshipscheduler.ui.views.scheduling.SchedulingViewPresenter;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Tag("scheduling-vis-timeline-panel")
@NpmPackage(value = "vis-timeline", version = "8.3.0")
@NpmPackage(value = "@js-joda/core", version = "5.6.5")
@JsModule("./src/components/scheduling-vis-timeline-panel.ts")
@JsModule("./src/components/by-factory-timeline-components.ts")
@JsModule("./src/components/by-order-timeline-components.ts")
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
        setPropertyNumber("dateTimeSlotSizeInMinute", townshipSchedulingProblem.getDateTimeSlotSize().getMinute());

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
                                .map(localDateTime -> localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                                .orElse("N/A")
                ))
                .toList();
    }

    private List<SchedulingFactoryInstanceVO> toFactoryInstanceVo(List<SchedulingFactoryInstance> schedulingFactoryInstanceList) {
        return schedulingFactoryInstanceList.stream()
                .sorted(Comparator.comparing(schedulingFactoryInstance -> schedulingFactoryInstance.getSchedulingFactoryInfo().getLevel()))
                .map(schedulingFactoryInstance -> {
                    Integer id = schedulingFactoryInstance.getId();
                    String categoryName = schedulingFactoryInstance.getCategoryName();
                    int seqNum = schedulingFactoryInstance.getSeqNum();
                    int producingLength = schedulingFactoryInstance.getProducingLength();
                    int reapWindowSize = schedulingFactoryInstance.getReapWindowSize();
                    FactoryReadableIdentifier factoryReadableIdentifier = schedulingFactoryInstance.getFactoryReadableIdentifier();

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

    private List<SchedulingProducingArrangementVO> toProducingArrangementVo(List<SchedulingProducingArrangement> schedulingProducingArrangementList) {
        return schedulingProducingArrangementList.stream()
                .map(producingArrangement -> {
                    return new SchedulingProducingArrangementVO(producingArrangement);
//                    return new SchedulingProducingArrangementVO(
//                            producingArrangement.getId(),
//                            producingArrangement.getUuid(),
//                            String.valueOf(producingArrangement.getSchedulingOrder().getId()),
//                            producingArrangement.getSchedulingProduct().getName(),
//                            Optional.ofNullable(planningFactoryInstance)
//                                    .map(schedulingFactoryInstance -> {
//                                        return schedulingFactoryInstance.getFactoryReadableIdentifier().toString();
//                                    })
//                                    .orElse(null),
//                            producingArrangement.getProducingDuration().toString(),
//                            producingArrangement.getArrangeDateTime(),
//                            producingArrangement.getProducingDateTime(),
//                            producingArrangement.getCompletedDateTime()
//                    );
                })
                .toList();

    }

    private void setPropertyNumber(String name, double value) {
        getElement().setProperty(name, value);
    }

    public void updateRemoteArrangements() {
        updateRemoteArrangements(this.schedulingViewPresenter.getTownshipSchedulingProblem());
    }

    private void updateRemoteArrangements(TownshipSchedulingProblem townshipSchedulingProblem) {

        setPropertyList(
                "schedulingProducingArrangements",
                toProducingArrangementVo(townshipSchedulingProblem.getSchedulingProducingArrangementList())
        );

    }

    private void setPropertyMap(String name, Map<String, ?> map) {
        getElement().setPropertyMap(name, map);
    }

    private void setPropertyObject(String name, Object object) {
        getElement().setPropertyBean(name, object);
    }

}
