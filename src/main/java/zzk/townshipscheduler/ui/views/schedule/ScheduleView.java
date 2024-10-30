package zzk.townshipscheduler.ui.views.schedule;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;

import java.util.Objects;

@Slf4j
@Route
@Menu(order = 5d)
@PageTitle("Township Scheduling")
public class ScheduleView extends VerticalLayout implements HasUrlParameter<String> {

    private ITownshipSchedulingService schedulingService;

    private transient String currentScheduleId;

    private ViewState viewState = ViewState.LIST;

    public ScheduleView() {
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        if (Objects.nonNull(parameter) && parameter.isBlank() && schedulingService.checkUuidIsValidForSchedule(parameter)) {
            currentScheduleId = parameter;
            viewState = ViewState.DETAIL;
        }
    }

    enum ViewState {
        LIST,
        DETAIL
    }

}
