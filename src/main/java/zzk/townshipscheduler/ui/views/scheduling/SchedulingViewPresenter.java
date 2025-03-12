package zzk.townshipscheduler.ui.views.scheduling;

import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.backend.scheduling.model.AbstractPlayerProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.List;
import java.util.UUID;

@SpringComponent
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

    public void setupPlayerActionGrid(Grid<AbstractPlayerProducingArrangement> grid) {
        grid.setItems(findCurrentProblem().getPlayerProducingArrangements());
    }

    private TownshipSchedulingProblem findCurrentProblem() {
        if (currentProblem == null) {
            this.currentProblem = schedulingService.getSchedule(getCurrentProblemId());
        }
        return this.currentProblem;
    }

    public void reset() {
        this.currentProblem = null;
        this.currentProblemId = null;
    }

    public List<SchedulingOrder> getSchedulingOrder() {
        return findCurrentProblem().getSchedulingOrderSet();
    }

    public void schedulingAndPush() {
        solverJob = solverManager.solveBuilder()
                .withProblemId(currentProblemId)
                .withProblem(currentProblem)
                .withFirstInitializedSolutionConsumer((townshipSchedulingProblem, isTerminatedEarly) -> {
                    List<AbstractPlayerProducingArrangement> schedulingActions
                            = townshipSchedulingProblem.getPlayerProducingArrangements();
                    ScoreAnalysis<BendableScore> scoreAnalysis
                            = solutionManager.analyze(
                            townshipSchedulingProblem
                    );
                    this.ui.access(
                            () -> {
                                Grid<AbstractPlayerProducingArrangement> grid = getSchedulingView().getActionGrid();
                                grid.setItems(schedulingActions);
                                grid.getDataProvider().refreshAll();
                                getSchedulingView().getScoreAnalysisParagraph().setText(scoreAnalysis.toString());
                            }
                    );
                })
                .withBestSolutionConsumer(
                        townshipSchedulingProblem -> {
                            List<AbstractPlayerProducingArrangement> schedulingActions
                                    = townshipSchedulingProblem.getPlayerProducingArrangements();
                            ScoreAnalysis<BendableScore> scoreAnalysis
                                    = solutionManager.analyze(
                                    townshipSchedulingProblem
                            );
                            this.ui.access(
                                    () -> {
                                        Grid<AbstractPlayerProducingArrangement> grid = getSchedulingView().getActionGrid();
                                        grid.setItems(schedulingActions);
                                        grid.getDataProvider().refreshAll();
                                        getSchedulingView().getScoreAnalysisParagraph()
                                                .setText(scoreAnalysis.toString());
                                    }
                            );
                        }
                )
                .withFinalBestSolutionConsumer(townshipSchedulingProblem -> {
                    this.currentProblem = townshipSchedulingProblem;
                    List<AbstractPlayerProducingArrangement> schedulingActions
                            = this.currentProblem.getPlayerProducingArrangements();
                    ScoreAnalysis<BendableScore> scoreAnalysis
                            = solutionManager.analyze(
                            townshipSchedulingProblem
                    );
                    this.ui.access(
                            () -> {
                                Grid<AbstractPlayerProducingArrangement> grid = getSchedulingView().getActionGrid();
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
