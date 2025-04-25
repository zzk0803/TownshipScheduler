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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@SpringComponent
@RouteScope
@RouteScopeOwner(SchedulingView.class)
@RequiredArgsConstructor
public class SchedulingViewPresenter {

    private final ITownshipSchedulingService schedulingService;

    private final SolverFactory<TownshipSchedulingProblem> solverFactory;

    private final SolverManager<TownshipSchedulingProblem, UUID> solverManager;

    private final SolutionManager<TownshipSchedulingProblem, BendableScore> solutionManager;

    @Getter
    @Setter
    private SolverJob<TownshipSchedulingProblem, UUID> solverJob;

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

    public void setupPlayerActionGrid(Grid<SchedulingProducingArrangement> grid) {
        grid.setItems(findCurrentProblem().getSchedulingProducingArrangementList());
    }

    public TownshipSchedulingProblem findCurrentProblem() {
        if (getTownshipSchedulingProblem() == null) {
            setTownshipSchedulingProblem(schedulingService.getSchedule(getCurrentProblemId()));
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

    public void schedulingAndPush() {
        Consumer<TownshipSchedulingProblem> solutionConsumer = townshipSchedulingProblem -> {
            SchedulingViewPresenter.this.setTownshipSchedulingProblem(townshipSchedulingProblem);
            List<SchedulingProducingArrangement> producingArrangements
                    = townshipSchedulingProblem.getSchedulingProducingArrangementList();
            ScoreAnalysis<BendableScore> scoreAnalysis
                    = solutionManager.analyze(
                    townshipSchedulingProblem
            );

            this.ui.access(() -> {
                getSchedulingView().getScoreAnalysisParagraph()
                        .setText(scoreAnalysis.toString());
                getSchedulingView().getArrangementGrid().setItems(producingArrangements);
                getSchedulingView().getArrangementGrid().getListDataView().refreshAll();
                getSchedulingView().getArrangementTimelinePanel()
                        .updateRemote(townshipSchedulingProblem);
            });
        };

        solverJob = solverManager.solveBuilder()
                .withProblemId(UUID.fromString(currentProblemId))
                .withProblem(townshipSchedulingProblem)
                .withBestSolutionConsumer(solutionConsumer)
                .withFinalBestSolutionConsumer(solutionConsumer.andThen(_->{
                    this.ui.access(() -> {
                        getSchedulingView().getTriggerButton().fromState2ToState1();
                        Notification notification = new Notification();
                        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        notification.setText("Scheduling Done");
                        notification.setPosition(Notification.Position.MIDDLE);
                        notification.setDuration(3000);
                        notification.open();
                    });
                }))
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
                })
                .run();
    }

    public void schedulingAbort() {
        solverManager.terminateEarly(UUID.fromString(currentProblemId));
    }

    public boolean validProblemId(String parameter) {
        return schedulingService.checkUuidIsValidForSchedule(parameter);
    }

}
