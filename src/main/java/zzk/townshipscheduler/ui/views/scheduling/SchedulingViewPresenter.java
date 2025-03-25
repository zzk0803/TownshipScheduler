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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.backend.scheduling.model.BaseProducingArrangement;
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
    private String currentProblemId;

    @Getter
    @Setter
    private TownshipSchedulingProblem currentProblem;

    @Getter
    @Setter
    private UI ui;

    @Getter
    @Setter
    private SchedulingView schedulingView;

    public void setupPlayerActionGrid(Grid<BaseProducingArrangement> grid) {
        grid.setItems(findCurrentProblem().getBaseProducingArrangements());
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
                .withProblemId(UUID.fromString(currentProblemId))
                .withProblem(currentProblem)
                .withBestSolutionConsumer(
                        townshipSchedulingProblem -> {
                            List<BaseProducingArrangement> schedulingActions
                                    = townshipSchedulingProblem.getBaseProducingArrangements();
                            ScoreAnalysis<BendableScore> scoreAnalysis
                                    = solutionManager.analyze(
                                    townshipSchedulingProblem
                            );
                            this.ui.access(
                                    () -> {
                                        Grid<BaseProducingArrangement> grid = getSchedulingView().getActionGrid();
                                        grid.setItems(schedulingActions);
                                        grid.getDataProvider().refreshAll();
                                        getSchedulingView().getScoreAnalysisParagraph()
                                                .setText(scoreAnalysis.toString());
                                    }
                            );
                        }
                )
                .withExceptionHandler((uuid, throwable) -> {
                    throwable.printStackTrace();
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
