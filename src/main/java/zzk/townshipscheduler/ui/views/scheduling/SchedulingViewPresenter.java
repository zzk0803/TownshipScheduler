package zzk.townshipscheduler.ui.views.scheduling;

import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;
import com.vaadin.flow.spring.annotation.SpringComponent;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.scheduling.TaskScheduler;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingServiceImpl;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.ui.components.SchedulingReportArticle;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@SpringComponent
@RouteScope
@RouteScopeOwner(SchedulingView.class)
@RequiredArgsConstructor
public class SchedulingViewPresenter {

    public static final int UPDATE_FREQUENCY = 3;

    private final TownshipSchedulingServiceImpl schedulingService;

    private final SolutionManager<TownshipSchedulingProblem, BendableScore> solutionManager;

    @Getter
    @Setter
    private String currentProblemId;

    @Getter
    @Setter
    private TownshipSchedulingProblem townshipSchedulingProblem;

    @Getter
    @Setter
    private UI ui;

    @Getter
    @Setter
    private SchedulingView schedulingView;

    private ScheduledFuture<?> springScheduledFuture;

    @Resource(name = "townshipTaskScheduler")
    private TaskScheduler taskScheduler;

    public void setupPlayerActionGrid(Grid<SchedulingProducingArrangement> grid) {
        grid.setItems(findCurrentProblem().getSchedulingProducingArrangementList());
    }

    public TownshipSchedulingProblem findCurrentProblem() {
        if (getTownshipSchedulingProblem() == null) {
            setTownshipSchedulingProblem(this.schedulingService.getSchedule(getCurrentProblemId()));
        }
        return getTownshipSchedulingProblem();
    }

    public void reset() {
        setTownshipSchedulingProblem(null);
        setCurrentProblemId(null);
    }

    public List<SchedulingOrder> getSchedulingOrder() {
        return findCurrentProblem().getSchedulingOrderList();
    }

    public void onStartButton() {
        Consumer<TownshipSchedulingProblem> solutionConsumer = townshipSchedulingProblem -> {
            SchedulingViewPresenter.this.setTownshipSchedulingProblem(townshipSchedulingProblem);
            List<SchedulingProducingArrangement> producingArrangements
                    = townshipSchedulingProblem.getSchedulingProducingArrangementList();
            ScoreAnalysis<BendableScore> scoreAnalysis
                    = solutionManager.analyze(
                    townshipSchedulingProblem
            );

            this.ui.access(
                    () -> {
                        getSchedulingView().getScoreAnalysisParagraph()
                                .setText(scoreAnalysis.toString());
                        getSchedulingView().getArrangementGrid().setItems(producingArrangements);
                        getSchedulingView().getArrangementGrid().getListDataView().refreshAll();
                    }
            );
        };

        springScheduledFuture = taskScheduler.scheduleAtFixedRate(
                () -> this.ui.access(
                        () -> getSchedulingView().getArrangementTimelinePanel().pullScheduleResult()
                ),
                Instant.now().plusSeconds(1),
                Duration.ofSeconds(UPDATE_FREQUENCY)
        );

        schedulingService.schedulingWithSolverManager(
                solverManager -> solverManager.solveBuilder()
                        .withProblemId(currentProblemId)
                        .withProblem(townshipSchedulingProblem)
                        .withBestSolutionConsumer(solutionConsumer)
                        .withFinalBestSolutionConsumer(
                                solutionConsumer.andThen(_ -> {
                                            this.ui.access(
                                                    () -> {
                                                        getSchedulingView().getTriggerButton().fromState2ToState1();
                                                        Notification notification = new Notification();
                                                        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                                                        notification.setText("Scheduling Done");
                                                        notification.setPosition(Notification.Position.MIDDLE);
                                                        notification.setDuration(3000);
                                                        notification.open();
                                                    }
                                            );
                                        })
                                        .andThen(finalTownshipProblemResult -> {
                                            this.ui.access(() -> {
                                                SchedulingReportArticle reportArticle
                                                        = new SchedulingReportArticle(finalTownshipProblemResult);
                                                this.schedulingView.getTabSheet().add("Report", reportArticle);
                                            });
                                        })
                                        .andThen(_ -> {
                                            springScheduledFuture.cancel(true);
                                            getSchedulingView().getArrangementTimelinePanel().cleanPushQueue();
                                        })
                        )
                        .withExceptionHandler((uuid, throwable) -> {
                            throwable.printStackTrace();
                            this.ui.access(() -> {
                                getSchedulingView().getTriggerButton().fromState2ToState1();
                                Notification notification = new Notification();
                                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                                notification.setText(throwable.toString());
                                notification.setPosition(Notification.Position.MIDDLE);
                                notification.setDuration(3000);
                                notification.open();
                            });
                            springScheduledFuture.cancel(true);
                            getSchedulingView().getArrangementTimelinePanel().cleanPushQueue();
                        })
                        .run()
        );

        ui.addDetachListener(detachEvent -> {
            springScheduledFuture.cancel(true);
            getSchedulingView().getArrangementTimelinePanel().cleanPushQueue();
            schedulingService.abort(currentProblemId);
        });
    }

    public void onStopButton() {
        if (springScheduledFuture != null) {
            springScheduledFuture.cancel(true);
        }
        getSchedulingView().getArrangementTimelinePanel().cleanPushQueue();
        schedulingService.abort(currentProblemId);
    }

    public boolean validProblemId(String parameter) {
        return schedulingService.checkUuidIsValidForSchedule(parameter);
    }

}
