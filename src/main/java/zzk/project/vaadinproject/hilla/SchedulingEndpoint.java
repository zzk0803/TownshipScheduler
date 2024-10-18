//package zzk.project.vaadinproject.hilla;
//
//import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
//import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
//import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
//import com.vaadin.hilla.Endpoint;
//import lombok.RequiredArgsConstructor;
//import zzk.project.vaadinproject.backend.scheduling.PackagingSchedule;
//import zzk.project.vaadinproject.ui.SchedulingViewPresenter;
//
//@Endpoint
//@RequiredArgsConstructor
//public class SchedulingEndpoint {
//
//    private final SchedulingViewPresenter presenter;
//
//    public ScoreAnalysis<HardMediumSoftLongScore> analyze(ScoreAnalysisFetchPolicy fetchPolicy) {
//        return presenter.analyze(fetchPolicy);
//    }
//
//    public void stopSolving() {
//        presenter.stopSolving();
//    }
//
//    public PackagingSchedule packagingSchedule() {
//        return presenter.packagingSchedule();
//    }
//
//    public void solve() {
//        presenter.solve();
//    }
//
//}
