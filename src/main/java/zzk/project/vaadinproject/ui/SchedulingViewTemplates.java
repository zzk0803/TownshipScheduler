package zzk.project.vaadinproject.ui;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import zzk.project.vaadinproject.backend.scheduling.PackagingSchedule;
import zzk.project.vaadinproject.backend.scheduling.PackagingScheduleHolder;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Route
@Menu(order = 5d)
@PageTitle("Township Scheduling")
@Tag("scheduling-view")
@NpmPackage(value = "vis-timeline", version = "7.7.3")
@NpmPackage(value = "@js-joda/core", version = "5.6.3")
@JsModule("./src/scheduling/scheduling-view.ts")
@JsModule("./src/scheduling/by-job-timeline-component.ts")
@JsModule("./src/scheduling/by-line-timeline-component.ts")
@JsModule("./src/scheduling/lit-vis-timeline.ts")
public class SchedulingViewTemplates
        extends LitTemplate {

    public static final String SINGLETON_SOLUTION_ID = "1";

    public static final String SESSION_SCHEDULING_SOLUTION_KEY = "schedulingViewRefreshTimerFuture";

    private final PackagingScheduleHolder packagingScheduleHolder;

    private final SolverManager<PackagingSchedule, String> solverManager;

    private final SolutionManager<PackagingSchedule, HardMediumSoftLongScore> solutionManager;

    private final TaskScheduler springTaskScheduler;

    private final ObjectMapper objectMapper;

    private final PropertyDescriptor<String, String> serverRefreshOnSolvingIntervalInSecond = PropertyDescriptors.propertyWithDefault(
            "serverRefreshOnSolvingIntervalInSecond",
            "2s"
    );

    @Value("${timefold.solver.termination.spent-limit}")
    private String timefoldComputingDuration;

    public SchedulingViewTemplates(
            PackagingScheduleHolder packagingScheduleHolder,
            SolverManager<PackagingSchedule, String> solverManager,
            SolutionManager<PackagingSchedule, HardMediumSoftLongScore> solutionManager,
            TaskScheduler springTaskScheduler,
            @Qualifier("java8EnhancedObjectMapper") ObjectMapper objectMapper
    ) {
        this.packagingScheduleHolder = packagingScheduleHolder;
        this.solverManager = solverManager;
        this.solutionManager = solutionManager;
        this.springTaskScheduler = springTaskScheduler;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        packagingSchedule();
    }

    @ClientCallable
    public void packagingSchedule() {
        SolverStatus solverStatus = solverManager.getSolverStatus(SINGLETON_SOLUTION_ID);
        PackagingSchedule packagingSchedule = packagingScheduleHolder.read();
        packagingSchedule.setSolverStatus(solverStatus);
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

    @ClientCallable
    public void stopSolving() {
        solverManager.terminateEarly(SINGLETON_SOLUTION_ID);
        try {
            ScheduledFuture<?> scheduledFuture = (ScheduledFuture<?>) VaadinSession.getCurrent().getAttribute(
                    SESSION_SCHEDULING_SOLUTION_KEY);
            scheduledFuture.cancel(true);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @ClientCallable
    public void solve() {
        solverManager.solveBuilder()
                .withProblemId(SINGLETON_SOLUTION_ID)
                .withProblemFinder(id -> packagingScheduleHolder.read())
                .withBestSolutionConsumer(packagingScheduleHolder::write)
                .run()
        ;

        if (VaadinSession.getCurrent().getAttribute(SESSION_SCHEDULING_SOLUTION_KEY) == null) {
            try {
                ScheduledFuture<?> scheduledFuture = springTaskScheduler.scheduleAtFixedRate(
                        UI.getCurrent().accessLater(this::packagingSchedule, null),
                        Duration.parse("PT"+getFlowRefreshOnSolvingIntervalInSecond().toUpperCase())
                );
                springTaskScheduler.schedule(
                        () -> scheduledFuture.cancel(true),
                        Instant.now().plus(Duration.parse("PT" + this.timefoldComputingDuration.toUpperCase()))
                );
                VaadinSession.getCurrent().setAttribute(SESSION_SCHEDULING_SOLUTION_KEY, scheduledFuture);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getFlowRefreshOnSolvingIntervalInSecond() {
        return get(serverRefreshOnSolvingIntervalInSecond);
    }

    public void setFlowRefreshOnSolvingIntervalInSecond(String flowRefreshOnSolvingIntervalInSecond) {
        set(this.serverRefreshOnSolvingIntervalInSecond, flowRefreshOnSolvingIntervalInSecond);
    }

}
