package zzk.project.vaadinproject.ui;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.internal.AllowInert;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Qualifier;
import zzk.project.vaadinproject.backend.scheduling.PackagingSchedule;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Route
@Menu(order = 5d)
@PageTitle("Township Scheduling")
@Tag("scheduling-view")
@NpmPackage(value = "vis-timeline", version = "7.7.3")
@NpmPackage(value = "@js-joda/core", version = "5.6.3")
@JsModule("./src/scheduling/scheduling-view.ts")
//@JsModule("./src/scheduling/by-job-timeline-component.ts")
//@JsModule("./src/scheduling/by-line-timeline-component.ts")
//@JsModule("./src/scheduling/lit-vis-timeline.ts")
public class SchedulingViewTemplates
        extends LitTemplate {

    private final SchedulingViewPresenter presenter;

    private final ObjectMapper objectMapper;

    public SchedulingViewTemplates(
            SchedulingViewPresenter presenter,
            @Qualifier("java8EnhancedObjectMapper") ObjectMapper objectMapper
    ) {
        this.presenter = presenter;
        this.objectMapper = objectMapper;
        presenter.setView(this);

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setupPropertyOrStateForFrontend();
        super.onAttach(attachEvent);
    }

    private void setupPropertyOrStateForFrontend() {
        PackagingSchedule packagingSchedule = presenter.packagingSchedule();
        getElement().setPropertyList("products", packagingSchedule.getProducts());
        getElement().setPropertyList("jobs", packagingSchedule.getJobs());
        getElement().setPropertyList("lines", packagingSchedule.getLines());
        getElement().setProperty(
                "fromDate",
                packagingSchedule.getWorkCalendar().getFromDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
        getElement().setProperty(
                "toDate",
                packagingSchedule.getWorkCalendar().getToDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
        getElement().setProperty("solverStatus", packagingSchedule.getSolverStatus().name());
        getElement().setProperty(
                "score",
                Optional.ofNullable(packagingSchedule.getScore()).map(HardMediumSoftLongScore::toString)
                        .orElse("")
        );
    }

    public SchedulingViewPresenter getPresenter() {
        return presenter;
    }

    /*
    @ClientCallable
    public ScoreAnalysis<HardMediumSoftLongScore> analyze(ScoreAnalysisFetchPolicy fetchPolicy) {
        return presenter.analyze(fetchPolicy);
    }
     */

    @ClientCallable
    @AllowInert
    public void stopSolving() {
        presenter.stopSolving();
    }

    @ClientCallable
    @AllowInert
    public void packagingSchedule() {
        this.setupPropertyOrStateForFrontend();
    }

    @ClientCallable
    @AllowInert
    public void solve() {
        presenter.solve();
    }

}
