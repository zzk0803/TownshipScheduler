package zzk.townshipscheduler.ui.views.scheduling;

import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.*;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@SpringComponent
@UIScope
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
    private UUID currentProblemId;

    @Getter
    @Setter
    private TownshipSchedulingProblem currentProblem;

    @Getter
    @Setter
    private UI ui;

    @Getter
    @Setter
    private SchedulingView schedulingView;

    public void setupPlayerActionGrid(Grid<SchedulingPlayerFactoryAction> grid) {
        grid.setItems(findCurrentProblem().getSchedulingPlayerFactoryActions());
    }

    private TownshipSchedulingProblem findCurrentProblem() {
        if (currentProblem == null) {
            this.currentProblem = schedulingService.getSchedule(getCurrentProblemId());
        }
        return this.currentProblem;
    }

    public Set<SchedulingOrder> getSchedulingOrder() {
        return findCurrentProblem().getSchedulingOrderSet();
    }

    public void schedulingAndPush() {
        solverJob = solverManager.solveBuilder()
                .withProblemId(currentProblemId)
                .withProblem(currentProblem)
                .withFirstInitializedSolutionConsumer((townshipSchedulingProblem, isTerminatedEarly) -> {
                    List<SchedulingPlayerFactoryAction> schedulingActions = townshipSchedulingProblem.getSchedulingPlayerFactoryActions();
                    ScoreAnalysis<BendableScore> scoreAnalysis = solutionManager.analyze(
                            townshipSchedulingProblem
                    );
                    this.ui.access(
                            () -> {
                                Grid<SchedulingPlayerFactoryAction> grid = getSchedulingView().getActionGrid();
                                grid.setItems(schedulingActions);
                                grid.getDataProvider().refreshAll();
                                getSchedulingView().getScoreAnalysisParagraph().setText(scoreAnalysis.toString());
                            }
                    );
                })
                .withBestSolutionConsumer(
                        townshipSchedulingProblem -> {
                            List<SchedulingPlayerFactoryAction> schedulingActions = townshipSchedulingProblem.getSchedulingPlayerFactoryActions();
                            ScoreAnalysis<BendableScore> scoreAnalysis = solutionManager.analyze(
                                    townshipSchedulingProblem
                            );
                            this.ui.access(
                                    () -> {
                                        Grid<SchedulingPlayerFactoryAction> grid = getSchedulingView().getActionGrid();
                                        grid.setItems(schedulingActions);
                                        grid.getDataProvider().refreshAll();
                                        getSchedulingView().getScoreAnalysisParagraph().setText(scoreAnalysis.toString());
                                    }
                            );
                        }
                )
                .withFinalBestSolutionConsumer(townshipSchedulingProblem -> {
                    this.currentProblem = townshipSchedulingProblem;
                    List<SchedulingPlayerFactoryAction> schedulingActions = this.currentProblem.getSchedulingPlayerFactoryActions();
                    ScoreAnalysis<BendableScore> scoreAnalysis = solutionManager.analyze(
                            townshipSchedulingProblem
                    );
                    this.ui.access(
                            () -> {
                                Grid<SchedulingPlayerFactoryAction> grid = getSchedulingView().getActionGrid();
                                grid.setItems(schedulingActions);
                                grid.getDataProvider().refreshAll();
                                getSchedulingView().getScoreAnalysisParagraph().setText(scoreAnalysis.toString());
                            }
                    );
                })
                .withExceptionHandler((uuid, throwable) -> {
                    throwable.printStackTrace();
                })
                .run();
    }

    public void schedulingAbort() {
        solverManager.terminateEarly(currentProblemId);
    }

}
