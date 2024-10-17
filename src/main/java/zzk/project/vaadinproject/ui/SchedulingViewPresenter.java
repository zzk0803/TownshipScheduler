package zzk.project.vaadinproject.ui;

import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import zzk.project.vaadinproject.backend.scheduling.PackagingSchedule;
import zzk.project.vaadinproject.backend.scheduling.PackagingScheduleRepository;

@Slf4j
@SpringComponent
@VaadinSessionScope
@RequiredArgsConstructor
class SchedulingViewPresenter {

    public static final String SINGLETON_SOLUTION_ID = "1";

    private final PackagingScheduleRepository repository;

    private final SolverManager<PackagingSchedule, String> solverManager;

    private final SolutionManager<PackagingSchedule, HardMediumSoftLongScore> solutionManager;

    private SchedulingViewTemplates view;

    public SchedulingViewTemplates getView() {
        return view;
    }

    public void setView(SchedulingViewTemplates view) {
        this.view = view;
    }

    public ScoreAnalysis<HardMediumSoftLongScore> analyze(ScoreAnalysisFetchPolicy fetchPolicy) {
        PackagingSchedule problem = repository.read();
        return fetchPolicy == null ? solutionManager.analyze(problem) : solutionManager.analyze(problem, fetchPolicy);
    }

    public void stopSolving() {
        solverManager.terminateEarly(SINGLETON_SOLUTION_ID);
    }


    public PackagingSchedule packagingSchedule() {
        // Get the solver status before loading the solution
        // to avoid the race condition that the solver terminates between them
        SolverStatus solverStatus = solverManager.getSolverStatus(SINGLETON_SOLUTION_ID);
        PackagingSchedule schedule = repository.read();
        schedule.setSolverStatus(solverStatus);
        log.info("{}", schedule);
        return schedule;
    }

    public void solve() {
        solverManager.solveBuilder()
                .withProblemId(SINGLETON_SOLUTION_ID)
                .withProblemFinder(id -> repository.read())
                .withBestSolutionConsumer(repository::write)
                .run()
        ;
    }

}
