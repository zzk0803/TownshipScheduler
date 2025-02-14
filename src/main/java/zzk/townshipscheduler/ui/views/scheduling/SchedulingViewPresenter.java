package zzk.townshipscheduler.ui.views.scheduling;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.*;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayerFactoryAction;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@SpringComponent
@VaadinSessionScope
@RequiredArgsConstructor
public class SchedulingViewPresenter {

    private final ITownshipSchedulingService schedulingService;

    private final SolverFactory<TownshipSchedulingProblem> solverFactory;

    private final SolverManager<TownshipSchedulingProblem, UUID> solverManager;

    private final SolutionManager<TownshipSchedulingProblem, BendableScore> simpleSolutionManager;

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
        List<SchedulingPlayerFactoryAction> factoryActions
                = findCurrentProblem().getSchedulingPlayerFactoryActions();
        solverJob = solverManager.solveBuilder()
                .withProblemId(currentProblemId)
                .withProblem(currentProblem)
                .withBestSolutionConsumer(
                        townshipSchedulingProblem -> {
                            List<SchedulingPlayerFactoryAction> schedulingActions = townshipSchedulingProblem.getSchedulingPlayerFactoryActions();
                            this.ui.access(
                                    () -> {
                                        Grid<SchedulingPlayerFactoryAction> grid = getSchedulingView().getActionGrid();
                                        grid.setItems(schedulingActions);
                                    }
                            );
                        }
                )
                .withFinalBestSolutionConsumer(townshipSchedulingProblem -> {
                    this.currentProblem = townshipSchedulingProblem;
                    List<SchedulingPlayerFactoryAction> schedulingActions = this.currentProblem.getSchedulingPlayerFactoryActions();
                    this.ui.access(
                            () -> {
                                Grid<SchedulingPlayerFactoryAction> grid = getSchedulingView().getActionGrid();
                                grid.setItems(schedulingActions);
                            }
                    );
                })
                .withExceptionHandler((uuid, throwable) -> {
                })
                .run();
    }

    public void schedulingAbort() {
        solverManager.terminateEarly(currentProblemId);
    }

}
