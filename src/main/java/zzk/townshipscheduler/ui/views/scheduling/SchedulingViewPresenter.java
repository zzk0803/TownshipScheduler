package zzk.townshipscheduler.ui.views.scheduling;

import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
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
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.dao.OrderEntityRepository;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingPrepareComponent;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingRequest;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingServiceImpl;
import zzk.townshipscheduler.backend.scheduling.model.DateTimeSlotSize;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.ui.components.SchedulingReportArticle;
import zzk.townshipscheduler.ui.components.TriggerButton;
import zzk.townshipscheduler.ui.pojo.SchedulingProblemVo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

@SpringComponent
@RouteScope
@RouteScopeOwner(SchedulingView.class)
@RequiredArgsConstructor
@Setter
@Getter
public class SchedulingViewPresenter {

    public static final int UPDATE_FREQUENCY = 3;

    private final OrderEntityRepository orderEntityRepository;

    private final TownshipSchedulingPrepareComponent townshipSchedulingPrepareComponent;

    private final TownshipSchedulingServiceImpl schedulingService;

    private final SolutionManager<TownshipSchedulingProblem, BendableScore> solutionManager;

    private final TransactionTemplate transactionTemplate;

    private TownshipAuthenticationContext townshipAuthenticationContext;

    private String currentProblemId;

    private TownshipSchedulingProblem townshipSchedulingProblem;

    private UI ui;

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
                                                        getSchedulingView().getTriggerButton().setToState1();
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
                                getSchedulingView().getTriggerButton().setToState2();
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

    public List<OrderEntity> allOrder() {
       return this.getTownshipAuthenticationContext()
                .getPlayerEntity()
                .map(orderEntityRepository::queryForOrderListView)
                .orElse(Collections.emptyList());
    }

    public String backendPrepareTownshipScheduling(
            Collection<OrderEntity> orderEntityList,
            DateTimeSlotSize dateTimeSlotSize,
            LocalDateTime workCalendarStart,
            LocalDateTime workCalendarEnd
    ) {
        PlayerEntity playerEntity = townshipAuthenticationContext.getPlayerEntity().orElseThrow();

        return transactionTemplate.execute(status -> {
            TownshipSchedulingRequest townshipSchedulingRequest
                    = townshipSchedulingPrepareComponent.buildTownshipSchedulingRequest(
                    playerEntity,
                    orderEntityList,
                    dateTimeSlotSize,
                    workCalendarStart,
                    workCalendarEnd
            );
            TownshipSchedulingProblem problem
                    = schedulingService.prepareScheduling(townshipSchedulingRequest);
            return problem.getUuid();
        });
    }

    public Collection<SchedulingProblemVo> allSchedulingProblem() {
        return this.schedulingService.allSchedulingProblem();
    }

    public void setButtonState(TriggerButton triggerButton) {
        getUi().access(() -> {
            TownshipSchedulingProblem currentProblem = this.findCurrentProblem();
            SolverStatus solverStatus = currentProblem.getSolverStatus();
            if (solverStatus == SolverStatus.NOT_SOLVING) {
                triggerButton.setToState1();
            } else {
                triggerButton.setToState2();
            }
        });
    }

}
