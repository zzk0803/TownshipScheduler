package zzk.townshipscheduler.ui.views.scheduling;

import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.scheduling.TaskScheduler;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.dao.OrderEntityRepository;
import zzk.townshipscheduler.backend.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingPrepareComponent;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingRequest;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingServiceImpl;
import zzk.townshipscheduler.backend.scheduling.model.DateTimeSlotSize;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.ui.components.TriggerButton;
import zzk.townshipscheduler.ui.pojo.SchedulingProblemVo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

@SpringComponent
@UIScope
@RequiredArgsConstructor
@Setter
@Getter
public class SchedulingViewPresenter {

    public static final int UPDATE_FREQUENCY = 3;

    private final OrderEntityRepository orderEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    private final TownshipSchedulingPrepareComponent townshipSchedulingPrepareComponent;

    private final TownshipSchedulingServiceImpl schedulingService;

    private TownshipAuthenticationContext townshipAuthenticationContext;

    private String townshipSchedulingProblemId;

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
        setTownshipSchedulingProblem(this.schedulingService.getSchedule(getTownshipSchedulingProblemId()));
        return getTownshipSchedulingProblem();
    }

    public void onStartButton() {
        Consumer<TownshipSchedulingProblem> solutionConsumer
                = townshipSchedulingProblem -> {
            SchedulingViewPresenter.this.setTownshipSchedulingProblem(townshipSchedulingProblem);
            List<SchedulingProducingArrangement> producingArrangements
                    = townshipSchedulingProblem.getSchedulingProducingArrangementList();
            ScoreAnalysis<BendableScore> scoreAnalysis
                    = getSchedulingService().analyze(townshipSchedulingProblem);

            this.ui.access(
                    () -> {
                        getSchedulingView().getTriggerButton().setToState2();
                        getSchedulingView().getScoreAnalysisParagraph()
                                .setText(scoreAnalysis.toString());
                        getSchedulingView().getArrangementGrid().setItems(producingArrangements);
                        getSchedulingView().getArrangementGrid().getListDataView().refreshAll();

                        getSchedulingView().getArrangementReportArticle()
                                .update(townshipSchedulingProblem);
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
                        .withProblemId(this.townshipSchedulingProblemId)
                        .withProblem(this.townshipSchedulingProblem)
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
                                        .andThen(_ -> {
                                            springScheduledFuture.cancel(true);
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
                        })
                        .run()
        );

        ui.addDetachListener(detachEvent -> {
            springScheduledFuture.cancel(true);
            schedulingService.abort(townshipSchedulingProblemId);
        });
    }

    public void onStopButton() {
        if (springScheduledFuture != null) {
            springScheduledFuture.cancel(true);
        }

        schedulingService.abort(townshipSchedulingProblemId);
    }

    public boolean validProblemId(String parameter) {
        return schedulingService.checkUuidIsValidForSchedule(parameter);
    }

    public List<OrderEntity> fetchPlayerOrders() {
        return this.getTownshipAuthenticationContext()
                .getPlayerEntity()
                .map(orderEntityRepository::queryForOrderListView)
                .orElse(Collections.emptyList());
    }

    public String backendPrepareTownshipScheduling(
            Collection<OrderEntity> orderEntityList,
            DateTimeSlotSize dateTimeSlotSize,
            LocalDateTime workCalendarStart,
            LocalDateTime workCalendarEnd,
            LocalTime sleepStartPickerValue,
            LocalTime sleepEndPickerValue
    ) {
        PlayerEntity playerEntity = townshipAuthenticationContext.getPlayerEntity().orElseThrow();

        TownshipSchedulingRequest townshipSchedulingRequest
                = townshipSchedulingPrepareComponent.buildTownshipSchedulingRequest(
                playerEntity,
                orderEntityList,
                dateTimeSlotSize,
                workCalendarStart,
                workCalendarEnd,
                sleepStartPickerValue,
                sleepEndPickerValue
        );
        TownshipSchedulingProblem problem
                = schedulingService.prepareScheduling(townshipSchedulingRequest);
        return problem.getUuid();
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

    public byte[] fetchProductImage(Long productId) {
        Optional<byte[]> productImage = productEntityRepository.queryProductImageById(productId);
        return productImage.orElse(null);
    }

    public byte[] fetchProductImage(String productName) {
        Optional<byte[]> bytes = productEntityRepository.queryProductImageByName(productName);
        return bytes.orElse(null);
    }


}
