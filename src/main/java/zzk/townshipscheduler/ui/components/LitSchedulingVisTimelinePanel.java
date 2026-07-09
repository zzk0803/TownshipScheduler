package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.signals.Signal;
import zzk.townshipscheduler.ui.pojo.scheduling.LitSchedulingProducingArrangementVO;
import zzk.townshipscheduler.ui.pojo.scheduling.SchedulingFactoryInstanceViewModel;
import zzk.townshipscheduler.ui.pojo.scheduling.reactive.ReactiveTownshipSchedulingProblemViewModel;
import zzk.townshipscheduler.ui.views.scheduling.SchedulingView;
import zzk.townshipscheduler.ui.views.scheduling.SchedulingViewPresenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag("scheduling-vis-timeline-panel")
@NpmPackage(value = "vis-timeline", version = "8.5.1")
@NpmPackage(value = "@js-joda/core", version = "6.0.1")
@JsModule("./components/scheduling-vis-timeline-panel.ts")
@JsModule("./components/by-factory-timeline-components.ts")
@JsModule("./components/by-order-timeline-components.ts")
@JsModule("./components/by-unit-timeline-components.ts")
@JsModule("./components/lit-vis-timeline.ts")
public class LitSchedulingVisTimelinePanel
        extends Component {

    private final SchedulingView schedulingView;

    private final SchedulingViewPresenter schedulingViewPresenter;

    private final Signal<List<LitSchedulingProducingArrangementVO>> litSchedulingProducingArrangementVoListSignal;

    public LitSchedulingVisTimelinePanel(
            SchedulingView schedulingView,
            SchedulingViewPresenter schedulingViewPresenter
    ) {
        this.schedulingView = schedulingView;
        this.schedulingViewPresenter = schedulingViewPresenter;

        this.litSchedulingProducingArrangementVoListSignal = this.schedulingView.getReactiveTownshipSchedulingProblemViewModelValueSignal()
                .map(
                        townshipSchedulingProblem -> {
                            if (townshipSchedulingProblem != null) {
                                return townshipSchedulingProblem.schedulingProducingArrangementReactiveViewModels().getValues()
                                        .map(schedulingProducingArrangement -> {
                                            SchedulingFactoryInstanceViewModel schedulingFactoryInstanceViewModel = schedulingProducingArrangement.assignedFactoryInstance().get();
                                            return new LitSchedulingProducingArrangementVO(
                                                    schedulingProducingArrangement.arrangementViewModelId().id(),
                                                    schedulingProducingArrangement.arrangementViewModelId()
                                                            .uuid(),
                                                    String.valueOf(schedulingProducingArrangement.order()
                                                            .id()),
                                                    schedulingProducingArrangement.product()
                                                            .name(),
                                                    schedulingProducingArrangement.orderProduct()
                                                            .name(),
                                                    schedulingProducingArrangement.orderProductArrangementId()
                                                            .id(),
                                                    schedulingProducingArrangement.boolDirectToOrder(),
                                                    schedulingFactoryInstanceViewModel != null
                                                            ? schedulingFactoryInstanceViewModel.factoryReadableIdentifier()
                                                            : "N/A",
                                                    schedulingProducingArrangement.producingDuration()
                                                            .toString(),
                                                    schedulingProducingArrangement.arrangeDateTime().get(),
                                                    schedulingProducingArrangement.producingDateTime().get(),
                                                    schedulingProducingArrangement.completedDateTime().get()
                                            );
                                        })
                                        .collect(Collectors.toCollection(ArrayList::new));
                            } else {
                                return List.of();
                            }
                        }
                );

        getElement().bindProperty(
                "schedulingProducingArrangements",
                this.litSchedulingProducingArrangementVoListSignal,
                null
        );
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        updateRemoteFull();
    }

    public void updateRemoteFull() {
        updateRemoteFull(this.schedulingViewPresenter.getTownshipSchedulingProblemViewModel());
    }

    private void updateRemoteFull(ReactiveTownshipSchedulingProblemViewModel problemViewModel) {
        setPropertyObject(
                "schedulingWorkCalendar",
                problemViewModel.schedulingWorkCalendar()
        );
        setPropertyList(
                "schedulingOrders",
                problemViewModel.toLitOrderVoList()
        );
        setPropertyList(
                "schedulingProducts",
                Arrays.asList(problemViewModel.schedulingProductViewModels().toArray())
        );
        setPropertyList(
                "schedulingFactoryInstances",
                problemViewModel.toLitFactoryInstanceVoList()
        );
        //        setPropertyList(
        //                "schedulingProducingArrangements",
        //                problemViewModel.toLitSchedulingProducingArrangementVoList()
        //        );
        setPropertyList(
                "schedulingProducingArrangementUnitGroups",
                problemViewModel.toProducingArrangementUnitGroupVoList()
        );
        setPropertyNumber(
                "dateTimeSlotSizeInMinute",
                problemViewModel.dateTimeSlotDurationInMinute()
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

    private void setPropertyNumber(
            String name,
            double value
    ) {
        getElement().setProperty(
                name,
                value
        );
    }

    @ClientCallable
    public void pullScheduleResult() {
        updateRemoteFull();
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
