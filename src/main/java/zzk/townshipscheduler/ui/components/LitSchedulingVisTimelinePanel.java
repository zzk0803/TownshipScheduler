package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.ui.pojo.SchedulingFactoryInstanceVO;
import zzk.townshipscheduler.ui.pojo.SchedulingProducingArrangementVO;
import zzk.townshipscheduler.ui.views.scheduling.SchedulingViewPresenter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Tag("scheduling-vis-timeline-panel")
@NpmPackage(value = "vis-timeline", version = "7.7.4")
@NpmPackage(value = "@js-joda/core", version = "5.6.5")
@JsModule("./src/components/scheduling-vis-timeline-panel.ts")
@JsModule("./src/components/by-factory-timeline-components.ts")
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
        updateRemote();
    }

    public void updateRemote() {
        updateRemote(this.schedulingViewPresenter.getTownshipSchedulingProblem());
    }

    private void updateRemote(TownshipSchedulingProblem townshipSchedulingProblem) {
        setPropertyList(
                "schedulingOrder",
                townshipSchedulingProblem.getSchedulingOrderList()
        );
        setPropertyList(
                "schedulingProduct",
                townshipSchedulingProblem.getSchedulingProductList()
        );
        setPropertyList(
                "schedulingFactory",
                toFactoryInstanceVo(townshipSchedulingProblem.getSchedulingFactoryInstanceList())
        );
        setPropertyList(
                "producingArrangements",
                toProducingArrangementVo(townshipSchedulingProblem.getSchedulingProducingArrangementList())
        );

    }

    private void setPropertyList(String name, List<?> listObject) {
        getElement().setPropertyList(name, listObject);
    }

    private List<SchedulingFactoryInstanceVO> toFactoryInstanceVo(List<SchedulingFactoryInstance> schedulingFactoryInstanceList) {
        return schedulingFactoryInstanceList.stream()
                .map(schedulingFactoryInstance -> {
                    Integer id = schedulingFactoryInstance.getId();
                    String categoryName = schedulingFactoryInstance.getCategoryName();
                    int seqNum = schedulingFactoryInstance.getSeqNum();
                    int producingLength = schedulingFactoryInstance.getProducingLength();
                    int reapWindowSize = schedulingFactoryInstance.getReapWindowSize();

                    return new SchedulingFactoryInstanceVO(
                            id,
                            categoryName,
                            seqNum,
                            producingLength,
                            reapWindowSize
                    );
                })
                .toList();
    }

    private List<SchedulingProducingArrangementVO> toProducingArrangementVo(List<SchedulingProducingArrangement> schedulingProducingArrangementList) {
        return schedulingProducingArrangementList.stream()
                .map(producingArrangement -> {
                    SchedulingFactoryInstance planningFactoryInstance = producingArrangement.getPlanningFactoryInstance();
                    return new SchedulingProducingArrangementVO(
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
                                    .map(SchedulingFactoryInstance::getId)
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
