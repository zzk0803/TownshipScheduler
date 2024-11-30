package zzk.townshipscheduler.ui.views.scheduling;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.Objects;
import java.util.UUID;

@Route("/scheduling")
@PermitAll
@Menu
public class SchedulingView extends VerticalLayout implements HasUrlParameter<String> {

    private final SchedulingViewTemplates schedulingViewTemplates;

    private final ITownshipSchedulingService schedulingService;

    private  VerticalLayout contentWrapper;

    private  String currentScheduleId;

    private ViewState viewState = ViewState.LIST;

    public SchedulingView(
            SchedulingViewTemplates schedulingViewTemplates,
            ITownshipSchedulingService schedulingService
    ) {
        this.schedulingService = schedulingService;
        this.schedulingViewTemplates = schedulingViewTemplates;
        schedulingViewTemplates.setSchedulingView(this);
        setHeightFull();

        contentWrapper = new VerticalLayout();
        contentWrapper.setSizeFull();
        addAndExpand(contentWrapper);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (viewState == ViewState.LIST) {
            Grid<TownshipSchedulingProblem> problemGrid = new Grid<>();
            problemGrid.addColumn(TownshipSchedulingProblem::getUuid).setHeader("uuid");
            problemGrid.addColumn(TownshipSchedulingProblem::getDateTime).setHeader("date-time");
            problemGrid.addColumn(TownshipSchedulingProblem::getSchedulingOrderList).setHeader("order");
            contentWrapper.addAndExpand(problemGrid);
        } else if (viewState == ViewState.DETAIL) {
            this.schedulingViewTemplates.setProblemId(UUID.fromString(this.currentScheduleId));
            contentWrapper.addAndExpand(this.schedulingViewTemplates);
        }else {
            contentWrapper.add(new H1("Scheduling View"));
        }
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (Objects.nonNull(parameter) && !parameter.isBlank() && schedulingService.checkUuidIsValidForSchedule(parameter)) {
            this.currentScheduleId = parameter;
            this.viewState = ViewState.DETAIL;
        }
    }

    enum ViewState {
        LIST,
        DETAIL
    }

}
