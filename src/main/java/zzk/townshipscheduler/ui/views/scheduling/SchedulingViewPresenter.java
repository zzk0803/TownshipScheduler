package zzk.townshipscheduler.ui.views.scheduling;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.persistence.dao.OrderEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingBenchmarkRequest;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingPrepareComponent;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingRequest;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.ui.components.ProductImages;
import zzk.townshipscheduler.ui.components.TriggerButton;
import zzk.townshipscheduler.ui.pojo.SchedulingOrderVo;
import zzk.townshipscheduler.ui.pojo.SchedulingProblemVo;
import zzk.townshipscheduler.ui.utility.VaadinUiEventBus;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@SpringComponent
@RouteScope
@RouteScopeOwner(SchedulingView.class)
@RequiredArgsConstructor
@Setter
@Getter
public class SchedulingViewPresenter {

    public static final int UPDATE_FREQUENCY_IN_SECONDS = 3;

    private final OrderEntityRepository orderEntityRepository;

    private final ProductEntityRepository productEntityRepository;

    private final TownshipSchedulingPrepareComponent townshipSchedulingPrepareComponent;

    private final ITownshipSchedulingService schedulingService;

    private UI ui;

    private SchedulingView schedulingView;

    private TownshipAuthenticationContext townshipAuthenticationContext;

    @Resource(name = "townshipTaskScheduler")
    private TaskScheduler taskScheduler;

    private String townshipSchedulingProblemId;

    private AtomicReference<TownshipSchedulingProblem> townshipSchedulingProblemAtomicReference = new AtomicReference<>();

    private AtomicReference<SolverJob<TownshipSchedulingProblem>> townshipSchedulingProblemSolverJobAtomicReference = new AtomicReference<>();

    private ScheduledFuture<?> solutionResultPushScheduledFuture;

    public Image getProductImage(String productName) {
        return createProductImage(productName);
    }

    public Image createProductImage(String productName) {
        byte[] productImage = fetchProductImage(productName);
        Image image = ProductImages.productImage(productName, productImage);
        image.setWidth("50px");
        image.setHeight("50px");

        return image;
    }

    public byte[] fetchProductImage(String productName) {
        Optional<byte[]> bytes = productEntityRepository.queryProductImageByName(productName);
        return bytes.orElse(null);
    }

    public void setupArrangementsGrid(Grid<SchedulingProducingArrangement> grid) {
        setupArrangementsGrid(grid, findCurrentProblem());
    }

    public void setupArrangementsGrid(Grid<SchedulingProducingArrangement> grid, TownshipSchedulingProblem townshipSchedulingProblem) {
        grid.setItems(townshipSchedulingProblem.getSchedulingProducingArrangements());
    }

    public TownshipSchedulingProblem findCurrentProblem() {
        return this.townshipSchedulingProblemAtomicReference.updateAndGet(_ -> SchedulingViewPresenter.this.schedulingService.gatherProblem(
                getTownshipSchedulingProblemId()));
    }

    public void onStartButton() {
        Consumer<TownshipSchedulingProblem> solutionConsumer = townshipSchedulingProblem -> {
            this.townshipSchedulingProblemAtomicReference.set(townshipSchedulingProblem);
        };

        var townshipSchedulingProblemSolverJob = schedulingService.scheduling(
                getTownshipSchedulingProblemId(),
                _ -> {
                    this.ui.access(() -> {
                        getSchedulingView().getTriggerButton()
                                .setToState2();
                        String problemSizeStatistics = getSchedulingService().getProblemSizeStatistics(getTownshipSchedulingProblemId());
                        String updatedString = getSchedulingView().getBriefText()
                                .getText() + "\r" + "solver approximate problem scale:" + problemSizeStatistics;
                        getSchedulingView().getBriefText()
                                .setText(updatedString);
                    });
                }, solutionConsumer,
                solutionConsumer.andThen(_ -> {
                            solutionResultPushScheduledFuture.cancel(true);
                        })
                        .andThen(_ -> this.ui.access(() -> {
                            getSchedulingView().getTriggerButton()
                                    .setToState1();
                            Notification notification = new Notification();
                            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            notification.setText("Township Solver Finished");
                            notification.setDuration(3000);
                            notification.open();
                            VaadinUiEventBus.publish(new SchedulingView.SchedulingProcessingEndComponentEvent(this.schedulingView, false));
                        })),
                (uuid, throwable) -> {
                    throwable.printStackTrace();
                    this.ui.access(() -> {
                        getSchedulingView().getTriggerButton()
                                .setToState1();
                        Dialog dialog = new Dialog("ERROR", new Paragraph(throwable.toString()));
                        dialog.open();
                    });
                    solutionResultPushScheduledFuture.cancel(true);
                }
        );
        this.townshipSchedulingProblemSolverJobAtomicReference.set(townshipSchedulingProblemSolverJob);

        this.solutionResultPushScheduledFuture = taskScheduler.scheduleAtFixedRate(
                () -> this.ui.access(() -> {
                    if (!getSchedulingService().existSolvingJob(getTownshipSchedulingProblemId())) {
                        this.solutionResultPushScheduledFuture.cancel(true);
                        getSchedulingView().getTriggerButton()
                                .setToState1();
                    }

                    TownshipSchedulingProblem townshipSchedulingProblem = this.getTownshipSchedulingProblemAtomicReference()
                            .get();
                    this.setupScoreFromProblem(townshipSchedulingProblem);
                    this.setupArrangementsTreeGrid(getSchedulingView().getArrangementTreeGrid(), townshipSchedulingProblem);
                    getSchedulingView().getArrangementReportArticle()
                            .push(townshipSchedulingProblem);
                    this.setupOrderBriefGrid();
                    getSchedulingView().getArrangementTimelinePanel()
                            .updateRemoteArrangements();
                }),
                Instant.now()
                        .plusSeconds(1),
                Duration.ofSeconds(UPDATE_FREQUENCY_IN_SECONDS)
        );

        VaadinUiEventBus.publish(new SchedulingView.SchedulingProcessingStartComponentEvent(schedulingView, false));
    }

    private void setupScoreFromProblem(TownshipSchedulingProblem townshipSchedulingProblem) {
        HardMediumSoftScore score = townshipSchedulingProblem.getScore();
        getSchedulingView().getScoreAnalysisParagraph()
                .setText(Objects.isNull(score) ? "N/A" : score.toString());
    }

    public void setupArrangementsTreeGrid(
            TreeGrid<SchedulingProducingArrangement> treeGrid,
            TownshipSchedulingProblem townshipSchedulingProblem
    ) {
        Collection<SchedulingProducingArrangement> arrangementList = townshipSchedulingProblem.getSchedulingProducingArrangements();
        treeGrid.setTreeData(toTreeData(arrangementList));
    }

    public void setupOrderBriefGrid() {
        List<SchedulingOrderVo> schedulingOrderVo = toSchedulingOrderVo();
        this.getSchedulingView()
                .getOrderBriefGrid()
                .setItems(schedulingOrderVo);
    }

    private TreeData<SchedulingProducingArrangement> toTreeData(Collection<SchedulingProducingArrangement> arrangementList) {
        TreeData<SchedulingProducingArrangement> arrangementTreeData = new TreeData<>();

        arrangementTreeData.addItems(
                arrangementList.stream()
                        .filter(SchedulingProducingArrangement::boolOrderDirect)
                        .toList(),
                SchedulingProducingArrangement::getPrerequisiteProducingArrangements
        );

        return arrangementTreeData;
    }

    public List<SchedulingOrderVo> toSchedulingOrderVo() {
        TownshipSchedulingProblem problem = findCurrentProblem();
        SchedulingWorkCalendar schedulingWorkCalendar = problem.getSchedulingWorkCalendar();
        List<SchedulingOrder> schedulingOrderList = problem.getSchedulingOrderList();
        Collection<SchedulingProducingArrangement> schedulingProducingArrangementList = problem.getSchedulingProducingArrangements();
        return schedulingOrderList.stream()
                .map(schedulingOrder -> {
                    SchedulingOrderVo schedulingOrderVo = new SchedulingOrderVo();
                    schedulingOrderVo.setSerial(Math.toIntExact(schedulingOrder.getId()));
                    schedulingOrderVo.setOrderType(schedulingOrder.getOrderType());
                    schedulingOrderVo.setProductAmountBill(schedulingOrder.getProductAmountBill());
                    schedulingOrderVo.setRelatedArrangements(schedulingProducingArrangementList.stream()
                            .filter(schedulingProducingArrangement -> schedulingOrder.equals(schedulingProducingArrangement.getSchedulingOrder()))
                            .toList());
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
        if (solutionResultPushScheduledFuture != null) {
            solutionResultPushScheduledFuture.cancel(true);
        }

        schedulingService.abort(townshipSchedulingProblemId);

        VaadinUiEventBus.publish(new SchedulingView.SchedulingProcessingEndComponentEvent(schedulingView, false));
    }

    public boolean checkWeatherReadyToSolve(String parameter) {
        return schedulingService.checkWeatherReadyToSolve(parameter);
    }

    public boolean checkWeatherProblemIsPersisted(String problemId) {
        return schedulingService.existProblem(problemId);
    }

    public List<OrderEntity> fetchPlayerOrders() {
        return this.getTownshipAuthenticationContext()
                .getPlayerEntity()
                .map(orderEntityRepository::queryForOrderListView)
                .orElse(Collections.emptyList());
    }

    public String backendPrepareTownshipScheduling(
            Collection<OrderEntity> orderEntityList, DateTimeSlotSize dateTimeSlotSize, LocalDateTime workCalendarStart,
//            LocalDateTime workCalendarEnd,
            LocalTime sleepStartPickerValue, LocalTime sleepEndPickerValue
    ) {
        PlayerEntity playerEntity = townshipAuthenticationContext.getPlayerEntity()
                .orElseThrow();

        TownshipSchedulingRequest townshipSchedulingRequest = townshipSchedulingPrepareComponent.buildTownshipSchedulingRequest(
                playerEntity, orderEntityList, dateTimeSlotSize, workCalendarStart,
//                workCalendarEnd,
                sleepStartPickerValue, sleepEndPickerValue
        );
        TownshipSchedulingProblem problem = schedulingService.prepareScheduling(townshipSchedulingRequest);
        return problem.getUuid();
    }

    public Collection<SchedulingProblemVo> viewFromLinkedSchedulingProblem() {
        Collection<TownshipSchedulingProblem> schedulingProblems = this.schedulingService.getLinkedSchedulingProblem();
        return toSchedulingProblemVO(schedulingProblems);
    }

    public Collection<SchedulingProblemVo> toSchedulingProblemVO(Collection<TownshipSchedulingProblem> townshipSchedulingProblemCollection) {
        return townshipSchedulingProblemCollection.stream()
                .map(problem -> {
                    SchedulingProblemVo schedulingProblemVo = new SchedulingProblemVo();
                    String uuid = problem.getUuid();
                    schedulingProblemVo.setUuid(uuid);
                    schedulingProblemVo.setSolverStatus(this.getSchedulingService()
                            .getProblemSolverStatus(uuid));
                    List<SchedulingOrder> orderList = problem.getSchedulingOrderList();
                    schedulingProblemVo.setOrderList(orderList);
                    return schedulingProblemVo;
                })
                .collect(Collectors.toCollection(HashSet::new));
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
        this.setupScoreFromProblem(this.townshipSchedulingProblemAtomicReference.get());
    }

    public void setupArrangementsTreeGrid(TreeGrid<SchedulingProducingArrangement> treeGrid) {
        setupArrangementsTreeGrid(treeGrid, findCurrentProblem());
    }

    public Paragraph buildBriefText() {
        TownshipSchedulingProblem currentProblem = findCurrentProblem();
        int orderSize = currentProblem.getSchedulingOrderList()
                .size();
        long orderItemProducingArrangementCount = currentProblem.getSchedulingProducingArrangements()
                .stream()
                .filter(SchedulingProducingArrangement::boolOrderDirect)
                .count();
        int totalItemProducingArrangementCount = currentProblem.getSchedulingProducingArrangements()
                .size();
        int dateTimeValueRangeCount = currentProblem.getSchedulingDateTimeSlots()
                .size();
        int factoryCount = currentProblem.getSchedulingFactoryInstanceList()
                .size();

        String formatted = (
                """
                        your township scheduling problem include %s order
                        there's %s final product item to make
                        include all materials need %s arrangement.
                        factory value range size:%s
                        date times slot size:%s
                """
        ).formatted(
                orderSize,
                orderItemProducingArrangementCount,
                totalItemProducingArrangementCount,
                factoryCount,
                dateTimeValueRangeCount
        );
        return new Paragraph(formatted);
    }

    public TownshipSchedulingProblem getTownshipSchedulingProblem() {
        return this.townshipSchedulingProblemAtomicReference.get();
    }

    public void loadProblem(String problemId) {
        this.schedulingService.load(problemId)
                .orElseThrow(IllegalArgumentException::new);
    }

    public CompletableFuture<Optional<File>> onBenchmarkStart(TownshipSchedulingBenchmarkRequest benchmarkRequest) {
        return schedulingService.benchmark(benchmarkRequest);
    }


}
