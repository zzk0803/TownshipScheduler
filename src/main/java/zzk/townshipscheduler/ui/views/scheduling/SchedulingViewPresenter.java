package zzk.townshipscheduler.ui.views.scheduling;

import ai.timefold.solver.core.api.solver.SolverStatus;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.timepicker.TimePicker;
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
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.ui.components.TriggerButton;
import zzk.townshipscheduler.ui.pojo.SchedulingOrderVo;
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

    public void setupArrangementsGrid(Grid<SchedulingProducingArrangement> grid) {
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

            this.ui.access(
                    () -> {
                        getSchedulingView().getTriggerButton().setToState2();
                        getSchedulingView().getScoreAnalysisParagraph()
                                .setText(getSchedulingService().analyze(townshipSchedulingProblem).toString());
                        getSchedulingView().getArrangementGrid()
                                .setItems(townshipSchedulingProblem.getSchedulingProducingArrangementList());
                        getSchedulingView().getArrangementGrid().getListDataView().refreshAll();
                        getSchedulingView().getArrangementReportArticle()
                                .update(townshipSchedulingProblem);
                        this.setupOrderBriefGrid();
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

        schedulingService.scheduling(
                getTownshipSchedulingProblemId(),
                solutionConsumer,
                solutionConsumer
                        .andThen(_ -> this.ui.access(
                                        () -> {
                                            getSchedulingView().getTriggerButton().setToState1();
                                            Notification notification = new Notification();
                                            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                                            notification.setText("Scheduling Done");
                                            notification.setPosition(Notification.Position.MIDDLE);
                                            notification.setDuration(3000);
                                            notification.open();
                                        }
                                )
                        )
                        .andThen(townshipSchedulingProblem -> {
                            springScheduledFuture.cancel(true);
                        })
                ,
                (uuid, throwable) -> {
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
                }
        );

        ui.addDetachListener(detachEvent -> {
            springScheduledFuture.cancel(true);
            schedulingService.abort(townshipSchedulingProblemId);
        });
    }

    public void setupOrderBriefGrid() {
        List<SchedulingOrderVo> schedulingOrderVo = toSchedulingOrderVo();
        this.getSchedulingView().getOrderBriefGrid().setItems(schedulingOrderVo);
    }

    public List<SchedulingOrderVo> toSchedulingOrderVo() {
        TownshipSchedulingProblem problem = findCurrentProblem();
        SchedulingWorkCalendar schedulingWorkCalendar = problem.getSchedulingWorkCalendar();
        List<SchedulingOrder> schedulingOrderList = problem.getSchedulingOrderList();
        List<SchedulingProducingArrangement> schedulingProducingArrangementList = problem.getSchedulingProducingArrangementList();
        return schedulingOrderList.stream()
                .map(schedulingOrder -> {
                    SchedulingOrderVo schedulingOrderVo = new SchedulingOrderVo();
                    schedulingOrderVo.setOrderType(schedulingOrder.getOrderType());
                    schedulingOrderVo.setProductAmountBill(schedulingOrder.getProductAmountBill());
                    schedulingOrderVo.setRelatedArrangements(
                            schedulingProducingArrangementList.stream()
                                    .filter(schedulingProducingArrangement -> schedulingOrder.equals(
                                            schedulingProducingArrangement.getSchedulingOrder()))
                                    .toList()
                    );
                    if (schedulingOrder.boolHasDeadline()) {
                        schedulingOrderVo.setDeadline(schedulingOrder.getDeadline());
                    } else {
                        schedulingOrderVo.setDeadline(schedulingWorkCalendar.getEndDateTime());
                    }
                    return schedulingOrderVo;
                })
                .toList();
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

    public void setupSlotSizeSelectReadValue(Select<DateTimeSlotSize> slotSizeSelect) {
        DateTimeSlotSize slotSize = findCurrentProblem().getSlotSize();
        slotSizeSelect.setValue(slotSize);
    }

    public void setupWorkCalendarStartPickerPickerReadValue(DateTimePicker workCalendarStartPickerPicker) {
        SchedulingWorkCalendar workCalendar = findCurrentProblem().getSchedulingWorkCalendar();
        workCalendarStartPickerPicker.setValue(workCalendar.getStartDateTime());
    }

    public void setupWorkCalendarEndPickerPickerReadValue(DateTimePicker workCalendarEndPickerPicker) {
        SchedulingWorkCalendar workCalendar = findCurrentProblem().getSchedulingWorkCalendar();
        workCalendarEndPickerPicker.setValue(workCalendar.getEndDateTime());
    }

    public void setupPlayerSleepStartPickerReadValue(TimePicker playerSleepStartPicker) {
        SchedulingPlayer schedulingPlayer = findCurrentProblem().getSchedulingPlayer();
        playerSleepStartPicker.setValue(schedulingPlayer.getSleepStart());
    }

    public void setupPlayerSleepEndPickerReadValue(TimePicker playerSleepEndPicker) {
        SchedulingPlayer schedulingPlayer = findCurrentProblem().getSchedulingPlayer();
        playerSleepEndPicker.setValue(schedulingPlayer.getSleepEnd());
    }

    public void setupScoreAnalysisParagraph() {
        getSchedulingView().getScoreAnalysisParagraph()
                .setText(getSchedulingService().analyze(townshipSchedulingProblem).toString());
    }

}
