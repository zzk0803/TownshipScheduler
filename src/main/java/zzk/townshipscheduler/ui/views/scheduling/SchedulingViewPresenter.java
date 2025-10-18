package zzk.townshipscheduler.ui.views.scheduling;

import ai.timefold.solver.core.api.solver.SolverStatus;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;
import com.vaadin.flow.spring.annotation.SpringComponent;
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
import zzk.townshipscheduler.ui.components.ProductImages;
import zzk.townshipscheduler.ui.components.TriggerButton;
import zzk.townshipscheduler.ui.pojo.SchedulingOrderVo;
import zzk.townshipscheduler.ui.pojo.SchedulingProblemVo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
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

    private final ProductEntityRepository productEntityRepository;

    private final TownshipSchedulingPrepareComponent townshipSchedulingPrepareComponent;

    private final TownshipSchedulingServiceImpl schedulingService;

    private TownshipAuthenticationContext townshipAuthenticationContext;

    private String townshipSchedulingProblemId;

    private TownshipSchedulingProblem townshipSchedulingProblem;

    private UI ui;

    private SchedulingView schedulingView;

    private ScheduledFuture<?> timelinePushScheduledFuture;

    @Resource(name = "townshipTaskScheduler")
    private TaskScheduler taskScheduler;

    private Map<String, Image> productToImageMap = new LinkedHashMap<>();

    public Image getProductImage(String productName) {
//        if (productToImageMap.containsKey(productName)) {
//            return productToImageMap.get(productName);
//        } else {
//            productToImageMap.put(productName, createProductImage(productName));
//            return productToImageMap.get(productName);
//        }
        return createProductImage(productName);
    }

    public Image createProductImage(String productName) {
        byte[] productImage = fetchProductImage(productName);
        Image image = ProductImages.productImage(
                productName,
                productImage
        );
        image.setWidth("40px");
        image.setHeight("40px");

        return image;
    }

    public byte[] fetchProductImage(String productName) {
        Optional<byte[]> bytes = productEntityRepository.queryProductImageByName(productName);
        return bytes.orElse(null);
    }

    public void setupArrangementsGrid(Grid<SchedulingProducingArrangement> grid) {
        setupArrangementsGrid(grid, findCurrentProblem());
    }

    public void setupArrangementsGrid(
            Grid<SchedulingProducingArrangement> grid,
            TownshipSchedulingProblem townshipSchedulingProblem
    ) {
        grid.setItems(townshipSchedulingProblem.getSchedulingProducingArrangementList());
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
                        this.setupArrangementsTreeGrid(
                                getSchedulingView().getArrangementTreeGrid(),
                                townshipSchedulingProblem
                        );
                        getSchedulingView().getArrangementReportArticle()
                                .update(townshipSchedulingProblem);
                        this.setupOrderBriefGrid();
                    }
            );
        };

        timelinePushScheduledFuture = taskScheduler.scheduleAtFixedRate(
                () -> this.ui.access(
                        () -> getSchedulingView().getArrangementTimelinePanel().updateRemoteArrangements()
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
                            timelinePushScheduledFuture.cancel(true);
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
                    timelinePushScheduledFuture.cancel(true);
                }
        );

        ui.addDetachListener(detachEvent -> {
            timelinePushScheduledFuture.cancel(true);
            schedulingService.abort(townshipSchedulingProblemId);
        });
    }

    public void setupArrangementsTreeGrid(
            TreeGrid<SchedulingProducingArrangement> treeGrid,
            TownshipSchedulingProblem townshipSchedulingProblem
    ) {
        List<SchedulingProducingArrangement> arrangementList = townshipSchedulingProblem.getSchedulingProducingArrangementList();
        treeGrid.setTreeData(toTreeData(arrangementList));
    }

    private TreeData<SchedulingProducingArrangement> toTreeData(List<SchedulingProducingArrangement> arrangementList) {
        TreeData<SchedulingProducingArrangement> arrangementTreeData
                = new TreeData<>();

        arrangementTreeData.addItems(
                arrangementList.stream()
                        .filter(SchedulingProducingArrangement::isOrderDirect)
                        .toList(),
                SchedulingProducingArrangement::getPrerequisiteProducingArrangements
        );

        return arrangementTreeData;
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
                    schedulingOrderVo.setSerial(Math.toIntExact(schedulingOrder.getId()));
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
        if (timelinePushScheduledFuture != null) {
            timelinePushScheduledFuture.cancel(true);
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

    public void setupSlotSizeSelectReadValue(Select<DateTimeSlotSize> slotSizeSelect) {
        DateTimeSlotSize slotSize = findCurrentProblem().getDateTimeSlotSize();
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

    public void setupArrangementsTreeGrid(TreeGrid<SchedulingProducingArrangement> treeGrid) {
        setupArrangementsTreeGrid(treeGrid, findCurrentProblem());
    }

    public Text setupBriefText() {
        TownshipSchedulingProblem currentProblem = findCurrentProblem();
        int orderSize = currentProblem.getSchedulingOrderList().size();
        long orderItemProducingArrangementCount = currentProblem.getSchedulingProducingArrangementList()
                .stream()
                .filter(SchedulingProducingArrangement::isOrderDirect)
                .count();
        int totalItemProducingArrangementCount = currentProblem.getSchedulingProducingArrangementList()
                .size();
        int dateTimeValueRangeCount = currentProblem.getSchedulingDateTimeSlots().size();
        int factoryCount = currentProblem.getSchedulingFactoryInstanceList().size();
        String formatted = "your township scheduling problem include %s order,contain %s final product item to make,and include all materials  need %s arrangement.factory value range size:%s,date times slot size:%s".formatted(
                orderSize,
                orderItemProducingArrangementCount,
                totalItemProducingArrangementCount,
                factoryCount,
                dateTimeValueRangeCount
        );
        return new Text(formatted);
    }

}
