package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.ui.pojo.ProducingArrangementVO;
import zzk.townshipscheduler.ui.views.scheduling.SchedulingViewPresenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Tag("scheduling-vis-timeline-panel")
@NpmPackage(value = "vis-timeline", version = "7.7.3")
@NpmPackage(value = "@js-joda/core", version = "5.6.3")
@JsModule("./scheduling-vis-timeline-panel.ts")
@JsModule("./by-factory-timeline-components.ts")
@JsModule("./lit-vis-timeline.ts")
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
        var townshipSchedulingProblem
                = schedulingViewPresenter.findCurrentProblem();
        updateRemote(townshipSchedulingProblem);
    }

    public void updateRemote(TownshipSchedulingProblem townshipSchedulingProblem) {
        setPropertyList(
                "schedulingOrder",
                new ArrayList<>(townshipSchedulingProblem.getSchedulingOrderSet())
        );
        setPropertyList(
                "schedulingProduct",
                new ArrayList<>(townshipSchedulingProblem.getSchedulingProductSet())
        );
        setPropertyList(
                "schedulingFactory",
                getSchedulingFactoryInstances(townshipSchedulingProblem)
        );
        setPropertyList(
                "producingArrangements",
                toProducingArrangementVo(townshipSchedulingProblem.getBaseProducingArrangements())
        );

    }

    private void setPropertyList(String name, List<?> listObject) {
        getElement().setPropertyList(name, listObject);
    }

    private List<BaseSchedulingFactoryInstance> getSchedulingFactoryInstances(TownshipSchedulingProblem townshipSchedulingProblem) {
        List<SchedulingFactoryInstanceTypeSlot> factoryInstanceTypeSlotList
                = townshipSchedulingProblem.getSchedulingFactoryInstanceTypeSlotList();
        List<SchedulingFactoryInstanceTypeQueue> factoryInstanceTypeQueueList
                = townshipSchedulingProblem.getSchedulingFactoryInstanceTypeQueueList();
        List<BaseSchedulingFactoryInstance> schedulingFactoryInstances
                = new ArrayList<>(factoryInstanceTypeSlotList.size() + factoryInstanceTypeQueueList.size());
        schedulingFactoryInstances.addAll(factoryInstanceTypeSlotList);
        schedulingFactoryInstances.addAll(factoryInstanceTypeQueueList);
        return schedulingFactoryInstances;
    }

    private List<ProducingArrangementVO> toProducingArrangementVo(List<BaseSchedulingProducingArrangement> baseSchedulingProducingArrangementList) {
        return baseSchedulingProducingArrangementList.stream()
                .map(producingArrangement -> {
                    BaseSchedulingFactoryInstance planningFactoryInstance = producingArrangement.getPlanningFactoryInstance();
                    return new ProducingArrangementVO(
                            producingArrangement.getId(),
                            producingArrangement.getUuid(),
                            producingArrangement.getSchedulingProduct().getName(),
                            Optional.ofNullable(planningFactoryInstance)
                                    .map(baseSchedulingFactoryInstance -> {
                                        return baseSchedulingFactoryInstance
                                                       .getCategoryName() + "#" + baseSchedulingFactoryInstance
                                                       .getSeqNum();
                                    })
                                    .orElse("N/A"),
                            Optional.ofNullable(planningFactoryInstance)
                                    .map(BaseSchedulingFactoryInstance::getId)
                                    .map(String::valueOf)
                                    .orElse("N/A"),
                            producingArrangement.getProducingDuration().toString(),
                            producingArrangement.getArrangeDateTime(),
                            producingArrangement.getProducingDateTime(),
                            producingArrangement.getCompletedDateTime()
                    );
                })
                .toList();

    }

    private void setPropertyMap(String name, Map<String, ?> map) {
        getElement().setPropertyMap(name, map);
    }

    private void setPropertyObject(String name, Object object) {
        getElement().setPropertyBean(name, object);
    }

}
