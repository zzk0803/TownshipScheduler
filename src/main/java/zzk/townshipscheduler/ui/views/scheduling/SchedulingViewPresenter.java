package zzk.townshipscheduler.ui.views.scheduling;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
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
import org.jspecify.annotations.NonNull;
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
import zzk.townshipscheduler.ui.pojo.SchedulingProducingArrangementViewModel;
import zzk.townshipscheduler.ui.pojo.TownshipSchedulingProblemViewModel;
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

    private TownshipSchedulingViewRecordComponent townshipSchedulingViewRecordComponent;

    private TownshipAuthenticationContext townshipAuthenticationContext;

    @Resource(name = "townshipTaskScheduler")
    private TaskScheduler taskScheduler;

    private String townshipSchedulingProblemId;

    private AtomicReference<TownshipSchedulingProblem> townshipSchedulingProblemAtomicReference = new AtomicReference<>();

    private AtomicReference<TownshipSchedulingProblemViewModel> townshipSchedulingProblemViewModelAtomicReference = new AtomicReference<>();

    private AtomicReference<SolverJob<TownshipSchedulingProblem>> townshipSchedulingProblemSolverJobAtomicReference = new AtomicReference<>();

    private ScheduledFuture<?> solutionResultPushScheduledFuture;

    public Image getProductImage(String productName) {
        return createProductImage(productName);
    }

    private Image createProductImage(String productName) {
        byte[] productImage = fetchProductImage(productName);
        Image image = ProductImages.productImage(
                productName,
                productImage
        );
        image.setWidth("50px");
        image.setHeight("50px");

        return image;
    }

    private byte[] fetchProductImage(String productName) {
        Optional<byte[]> bytes = productEntityRepository.queryProductImageByName(productName);
        return bytes.orElse(null);
    }

    //    public void setupArrangementsGrid(Grid<SchedulingProducingArrangement> grid) {
    //        setupArrangementsGrid(
    //                grid,
    //                reflushAndGetCurrentProblem()
    //        );
    //    }
    //
    //    public void setupArrangementsGrid(
    //            Grid<SchedulingProducingArrangement> grid,
    //            TownshipSchedulingProblem townshipSchedulingProblem
    //    ) {
    //        grid.setItems(townshipSchedulingProblem.getSchedulingProducingArrangements());
    //    }

    public void onStartButton() {
        Consumer<TownshipSchedulingProblem> solutionConsumer = this::reflushAndGetCurrentProblem;

        SolverJob<TownshipSchedulingProblem> townshipSchedulingProblemSolverJob = schedulingService.scheduling(
                getTownshipSchedulingProblemId(),
                townshipSchedulingProblem -> {
                    this.ui.access(() -> {
                        solutionConsumer.accept(townshipSchedulingProblem);
                        getSchedulingView().getTriggerButton().setToState2();
                        String problemSizeStatistics = getSchedulingService().getProblemSizeStatistics(getTownshipSchedulingProblemId());
                        String updatedString = getSchedulingView().getBriefText().getText() + "\r" + "solver approximate problem scale:" + problemSizeStatistics;
                        getSchedulingView().getBriefText().setText(updatedString);
                    });
                },
                solutionConsumer.andThen(this::reflushAndGetViewModel),
                solutionConsumer.andThen(this::reflushAndGetViewModel).andThen(_ -> {
                    solutionResultPushScheduledFuture.cancel(true);
                }).andThen(_ -> this.ui.access(() -> {
                    getSchedulingView().getTriggerButton().setToState1();
                    Notification notification = new Notification();
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    notification.setText("Township Solver Finished");
                    notification.setDuration(3000);
                    notification.open();
                    VaadinUiEventBus.publish(new SchedulingView.SchedulingProcessingEndComponentEvent(
                            this.schedulingView,
                            false,
                            getTownshipSchedulingProblem().getUuid()
                    ));
                })),
                (problemUuid, throwable) -> {
                    this.ui.access(() -> {
                        getSchedulingView().getTriggerButton().setToState1();
                        Dialog dialog = new Dialog(
                                "ERROR",
                                new Paragraph(throwable.toString())
                        );
                        dialog.open();
                    });
                    solutionResultPushScheduledFuture.cancel(true);
                }
        );
        this.townshipSchedulingProblemSolverJobAtomicReference.set(townshipSchedulingProblemSolverJob);

        this.solutionResultPushScheduledFuture = taskScheduler.scheduleAtFixedRate(
                pushSolverResult(),
                Instant.now().plusSeconds(1),
                Duration.ofSeconds(UPDATE_FREQUENCY_IN_SECONDS)
        );

        VaadinUiEventBus.publish(new SchedulingView.SchedulingProcessingStartComponentEvent(
                schedulingView,
                false
        ));
    }

    private @NonNull Runnable pushSolverResult() {
        return () -> this.ui.access(() -> {
            if (!getSchedulingService().existSolvingJob(getTownshipSchedulingProblemId())) {
                this.solutionResultPushScheduledFuture.cancel(true);
                getSchedulingView().getTriggerButton().setToState1();
            }

            TownshipSchedulingProblemViewModel townshipSchedulingProblemViewModel = this.getTownshipSchedulingProblemViewModel();
            this.setupScoreFromProblem(townshipSchedulingProblemViewModel);
            this.setupOrderBriefGrid();
            this.setupArrangementsTreeGrid(
                    getSchedulingView().getArrangementTreeGrid(),
                    townshipSchedulingProblemViewModel.schedulingProducingArrangementViewModels()
            );
            this.getSchedulingView().getArrangementTimelinePanel().updateRemoteArrangements();
            this.getSchedulingView().getArrangementReportArticle().updateScheduling(townshipSchedulingProblemViewModel);
        });
    }

    public void setupOrderBriefGrid() {
        List<SchedulingOrderVo> schedulingOrderVo = toSchedulingOrderVo();
        this.getSchedulingView().getOrderBriefGrid().setItems(schedulingOrderVo);
    }

    public List<SchedulingOrderVo> toSchedulingOrderVo() {
        TownshipSchedulingProblem problem = reflushAndGetCurrentProblem();
        SchedulingWorkCalendar schedulingWorkCalendar = problem.getSchedulingWorkCalendar();
        List<SchedulingOrder> schedulingOrderList = problem.getSchedulingOrderList();
        Collection<SchedulingProducingArrangement> schedulingProducingArrangementList = problem.getSchedulingProducingArrangements();
        return schedulingOrderList.stream().map(schedulingOrder -> {
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
        }).toList();
    }

    public TownshipSchedulingProblem reflushAndGetCurrentProblem() {
        return this.townshipSchedulingProblemAtomicReference.updateAndGet(_ -> this.schedulingService.gatherProblem(getTownshipSchedulingProblemId()));
    }

    private void setupScoreFromProblem(TownshipSchedulingProblemViewModel townshipSchedulingProblem) {
        getSchedulingView().getScoreAnalysisParagraph().setText(townshipSchedulingProblem.score());
    }

    public void setupArrangementsTreeGrid(
            TreeGrid<SchedulingProducingArrangementViewModel> treeGrid,
            Collection<SchedulingProducingArrangementViewModel> schedulingProducingArrangementViewModels
    ) {
        treeGrid.setTreeData(toTreeData(schedulingProducingArrangementViewModels));
    }

    private TreeData<SchedulingProducingArrangementViewModel> toTreeData(Collection<SchedulingProducingArrangementViewModel> arrangementList) {
        TreeData<SchedulingProducingArrangementViewModel> arrangementTreeData = new TreeData<>();
        arrangementTreeData.addItems(
                arrangementList.stream().filter(SchedulingProducingArrangementViewModel::boolDirectToOrder),
                parentArrangement -> arrangementList.stream().filter(parentArrangement::boolChild)
        );
        return arrangementTreeData;
    }

    public TownshipSchedulingProblemViewModel getTownshipSchedulingProblemViewModel() {
        return this.townshipSchedulingProblemViewModelAtomicReference.get();
    }

    public TownshipSchedulingProblemViewModel reflushAndGetViewModel(TownshipSchedulingProblem townshipSchedulingProblem) {
        return this.townshipSchedulingProblemViewModelAtomicReference.updateAndGet(
                townshipSchedulingProblemViewModel -> this.townshipSchedulingViewRecordComponent.updateAndGet(
                        reflushAndGetCurrentProblem(townshipSchedulingProblem),
                        townshipSchedulingProblemViewModel
                )
        );
    }

    public TownshipSchedulingProblem reflushAndGetCurrentProblem(TownshipSchedulingProblem townshipSchedulingProblem) {
        return this.townshipSchedulingProblemAtomicReference.updateAndGet(_ -> townshipSchedulingProblem);
    }

    public TownshipSchedulingProblem getTownshipSchedulingProblem() {
        return this.townshipSchedulingProblemAtomicReference.get();
    }

    public TownshipSchedulingProblemViewModel reflushAndGetViewModel() {
        TownshipSchedulingProblem townshipSchedulingProblem = getTownshipSchedulingProblem();
        return this.reflushAndGetViewModel(townshipSchedulingProblem);
    }

    public void onStopButton() {
        if (solutionResultPushScheduledFuture != null) {
            solutionResultPushScheduledFuture.cancel(true);
        }

        schedulingService.abort(townshipSchedulingProblemId);
    }

    public boolean checkWeatherReadyToSolve(String parameter) {
        return schedulingService.checkWeatherReadyToSolve(parameter);
    }

    public boolean checkWeatherProblemIsPersisted(String problemId) {
        return schedulingService.existProblem(problemId);
    }

    public List<OrderEntity> fetchPlayerOrders() {
        return this.getTownshipAuthenticationContext().getPlayerEntity().map(orderEntityRepository::queryForOrderListView).orElse(Collections.emptyList());
    }

    public String backendPrepareTownshipScheduling(
            Collection<OrderEntity> orderEntityList,
            DateTimeSlotSize dateTimeSlotSize,
            LocalDateTime workCalendarStart,
            LocalTime sleepStartPickerValue,
            LocalTime sleepEndPickerValue
    ) {
        PlayerEntity playerEntity = townshipAuthenticationContext.getPlayerEntity().orElseThrow();

        TownshipSchedulingRequest townshipSchedulingRequest = townshipSchedulingPrepareComponent.buildTownshipSchedulingRequest(
                playerEntity,
                orderEntityList,
                dateTimeSlotSize,
                workCalendarStart,
                sleepStartPickerValue,
                sleepEndPickerValue
        );
        TownshipSchedulingProblem problem = schedulingService.prepareScheduling(townshipSchedulingRequest);
        return problem.getUuid();
    }

    public Collection<SchedulingProblemVo> viewFromLinkedSchedulingProblem() {
        Collection<TownshipSchedulingProblem> schedulingProblems = this.schedulingService.getLinkedSchedulingProblem();
        return toSchedulingProblemVO(schedulingProblems);
    }

    public Collection<SchedulingProblemVo> toSchedulingProblemVO(Collection<TownshipSchedulingProblem> townshipSchedulingProblemCollection) {
        return townshipSchedulingProblemCollection.stream().map(problem -> {
            SchedulingProblemVo schedulingProblemVo = new SchedulingProblemVo();
            String uuid = problem.getUuid();
            schedulingProblemVo.setUuid(uuid);
            schedulingProblemVo.setSolverStatus(this.getSchedulingService().getProblemSolverStatus(uuid));
            List<SchedulingOrder> orderList = problem.getSchedulingOrderList();
            schedulingProblemVo.setOrderList(orderList);
            return schedulingProblemVo;
        }).collect(Collectors.toCollection(HashSet::new));
    }

    public void setButtonState(TriggerButton triggerButton) {
        getUi().access(() -> {
            TownshipSchedulingProblem currentProblem = this.reflushAndGetCurrentProblem();
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
        DateTimeSlotSize slotSize = reflushAndGetCurrentProblem().getDateTimeSlotSize();
        slotSizeSelect.setValue(slotSize);
    }

    public void setupWorkCalendarStartPickerPickerReadValue(DateTimePicker workCalendarStartPickerPicker) {
        SchedulingWorkCalendar workCalendar = reflushAndGetCurrentProblem().getSchedulingWorkCalendar();
        workCalendarStartPickerPicker.setValue(workCalendar.getStartDateTime());
    }

    public void setupWorkCalendarEndPickerPickerReadValue(DateTimePicker workCalendarEndPickerPicker) {
        SchedulingWorkCalendar workCalendar = reflushAndGetCurrentProblem().getSchedulingWorkCalendar();
        workCalendarEndPickerPicker.setValue(workCalendar.getEndDateTime());
    }

    public void setupPlayerSleepStartPickerReadValue(TimePicker playerSleepStartPicker) {
        SchedulingPlayer schedulingPlayer = reflushAndGetCurrentProblem().getSchedulingPlayer();
        playerSleepStartPicker.setValue(schedulingPlayer.getSleepStart());
    }

    public void setupPlayerSleepEndPickerReadValue(TimePicker playerSleepEndPicker) {
        SchedulingPlayer schedulingPlayer = reflushAndGetCurrentProblem().getSchedulingPlayer();
        playerSleepEndPicker.setValue(schedulingPlayer.getSleepEnd());
    }

    public void setupScoreAnalysisParagraph() {
        this.setupScoreFromProblem(this.townshipSchedulingProblemViewModelAtomicReference.get());
    }

    public void setupArrangementsTreeGrid(TreeGrid<SchedulingProducingArrangementViewModel> treeGrid) {
        setupArrangementsTreeGrid(
                treeGrid,
                getTownshipSchedulingProblemViewModel().schedulingProducingArrangementViewModels()
        );
    }

    public Paragraph buildBriefText() {
        TownshipSchedulingProblem currentProblem = reflushAndGetCurrentProblem();
        int orderSize = currentProblem.getSchedulingOrderList().size();
        long orderItemProducingArrangementCount = currentProblem.getSchedulingProducingArrangements().stream().filter(SchedulingProducingArrangement::boolOrderDirect).count();
        int totalItemProducingArrangementCount = currentProblem.getSchedulingProducingArrangements().size();
        int dateTimeValueRangeCount = currentProblem.getSchedulingDateTimeSlots().size();
        int factoryCount = currentProblem.getSchedulingFactoryInstanceList().size();

        String formatted = ("""
                        your township scheduling problem include %s order
                        there's %s final product item to make
                        include all materials need %s arrangement.
                        factory value range size:%s
                        date times slot size:%s
                """).formatted(
                orderSize,
                orderItemProducingArrangementCount,
                totalItemProducingArrangementCount,
                factoryCount,
                dateTimeValueRangeCount
        );
        return new Paragraph(formatted);
    }

    public void loadProblem(String problemId) {
        this.schedulingService.load(problemId).orElseThrow(IllegalArgumentException::new);
    }

    public CompletableFuture<File> onBenchmarkStart(TownshipSchedulingBenchmarkRequest benchmarkRequest) {
        return schedulingService.benchmark(benchmarkRequest);
    }

}
