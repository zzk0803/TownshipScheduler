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
import com.vaadin.flow.signals.local.ValueSignal;
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
import zzk.townshipscheduler.ui.pojo.TownshipSchedulingProblemBriefViewModel;
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
        Image image = ProductImages.productImage(productName, productImage);
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

    public ValueSignal<TownshipSchedulingProblemViewModel> getTownshipSchedulingProblemViewModelSignal() {
        return this.getSchedulingView().getTownshipSchedulingProblemViewModelSignal();
    }

    public void onStartButton() {
        Consumer<TownshipSchedulingProblem> solutionConsumer = this::reflushAndGetCurrentProblem;

        SolverJob<TownshipSchedulingProblem> townshipSchedulingProblemSolverJob = schedulingService.scheduling(
                getTownshipSchedulingProblemId(), townshipSchedulingProblem -> {
                    this.ui.access(() -> {
                        solutionConsumer.accept(townshipSchedulingProblem);
                        getSchedulingView().getTriggerButton().setToState2();
                        // getSchedulingView().getSolverRunningSignal().set(true);
                        String problemSizeStatistics = getSchedulingService().getProblemSizeStatistics(getTownshipSchedulingProblemId());
                        String updatedString = getSchedulingView().getBriefText().getText() + "\r" + "solver approximate problem scale:" + problemSizeStatistics;
                        getSchedulingView().getBriefText().setText(updatedString);
                    });
                }, solutionConsumer.andThen(this::reflushAndGetViewModel), solutionConsumer.andThen(this::reflushAndGetViewModel).andThen(_ -> {
                    solutionResultPushScheduledFuture.cancel(true);
                }).andThen(_ -> this.ui.access(() -> {
                    getSchedulingView().getTriggerButton().setToState1();
                    Notification notification = new Notification();
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    notification.setText("Township Solver Finished");
                    notification.setDuration(3000);
                    notification.open();
                    VaadinUiEventBus.publish(new SchedulingView.SchedulingProcessingEndComponentEvent(this.schedulingView, false, getTownshipSchedulingProblem().getUuid()));
                })), (problemUuid, throwable) -> {
                    this.ui.access(() -> {
                        getSchedulingView().getTriggerButton().setToState1();
                        // getSchedulingView().getSolverRunningSignal().set(false);
                        Dialog dialog = new Dialog("ERROR", new Paragraph(throwable.toString()));
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

        VaadinUiEventBus.publish(new SchedulingView.SchedulingProcessingStartComponentEvent(schedulingView, false));
    }

    private @NonNull Runnable pushSolverResult() {
        return () -> this.ui.access(() -> {
            if (!getSchedulingService().existSolvingJob(getTownshipSchedulingProblemId())) {
                this.solutionResultPushScheduledFuture.cancel(true);
                getSchedulingView().getTriggerButton().setToState1();
                // getSchedulingView().getSolverRunningSignal().set(false);
            }

            signalTownshipSchedulingProblemViewModel(this.getTownshipSchedulingProblemViewModel());
        });
    }

    public void signalTownshipSchedulingProblemViewModel(TownshipSchedulingProblemViewModel townshipSchedulingProblemViewModel) {
        getSchedulingView().getTownshipSchedulingProblemViewModelSignal().set(townshipSchedulingProblemViewModel);
    }

    public TownshipSchedulingProblemViewModel getTownshipSchedulingProblemViewModel() {
        return this.townshipSchedulingProblemViewModelAtomicReference.get();
    }

    public void reflushAndGetViewModel(TownshipSchedulingProblem townshipSchedulingProblem) {
        this.townshipSchedulingProblemViewModelAtomicReference.updateAndGet(
                townshipSchedulingProblemViewModel -> this.townshipSchedulingViewRecordComponent.updateAndGet(
                        reflushAndGetCurrentProblem(townshipSchedulingProblem), townshipSchedulingProblemViewModel));
    }

    public TownshipSchedulingProblem reflushAndGetCurrentProblem(TownshipSchedulingProblem townshipSchedulingProblem) {
        return this.townshipSchedulingProblemAtomicReference.updateAndGet(_ -> townshipSchedulingProblem);
    }

    public TownshipSchedulingProblem getTownshipSchedulingProblem() {
        return this.townshipSchedulingProblemAtomicReference.get();
    }

    public void signalTownshipSchedulingProblemViewModel() {
        getSchedulingView().getTownshipSchedulingProblemViewModelSignal().set(getTownshipSchedulingProblemViewModel());
    }

    public void reflushAndGetViewModel() {
        TownshipSchedulingProblem townshipSchedulingProblem = getTownshipSchedulingProblem();
        this.reflushAndGetViewModel(townshipSchedulingProblem);
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
            Collection<OrderEntity> orderEntityList, DateTimeSlotSize dateTimeSlotSize, LocalDateTime workCalendarStart, LocalTime sleepStartPickerValue,
            LocalTime sleepEndPickerValue
    ) {
        PlayerEntity playerEntity = townshipAuthenticationContext.getPlayerEntity().orElseThrow();

        TownshipSchedulingRequest townshipSchedulingRequest = townshipSchedulingPrepareComponent.buildTownshipSchedulingRequest(
                playerEntity, orderEntityList, dateTimeSlotSize, workCalendarStart,
                sleepStartPickerValue, sleepEndPickerValue
        );
        TownshipSchedulingProblem problem = schedulingService.prepareScheduling(townshipSchedulingRequest);
        return problem.getUuid();
    }

    public Collection<TownshipSchedulingProblemBriefViewModel> viewFromLinkedSchedulingProblem() {
        Collection<TownshipSchedulingProblem> schedulingProblems = this.schedulingService.getLinkedSchedulingProblem();
        return toTownshipSchedulingProblemBriefViewModel(schedulingProblems);
    }

    public Collection<TownshipSchedulingProblemBriefViewModel> toTownshipSchedulingProblemBriefViewModel(Collection<TownshipSchedulingProblem> townshipSchedulingProblemCollection) {
        return townshipSchedulingProblemCollection.stream().map(problem -> {
            String uuid = problem.getUuid();
            TownshipSchedulingViewRecordComponent otherProblemViewRecordComponent = townshipSchedulingViewRecordComponent.forOtherProblem(problem);

            return new TownshipSchedulingProblemBriefViewModel(
                    uuid, this.getSchedulingService().getProblemSolverStatus(uuid).name(),
                    otherProblemViewRecordComponent.mapAndGetSchedulingOrderViewModel()
            );
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

    public TownshipSchedulingProblem reflushAndGetCurrentProblem() {
        return this.townshipSchedulingProblemAtomicReference.updateAndGet(_ -> this.schedulingService.gatherProblem(getTownshipSchedulingProblemId()));
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
                            """).formatted(orderSize, orderItemProducingArrangementCount, totalItemProducingArrangementCount, factoryCount, dateTimeValueRangeCount);
        return new Paragraph(formatted);
    }

    public void loadProblem(String problemId) {
        this.schedulingService.load(problemId).orElseThrow(IllegalArgumentException::new);
    }

    public CompletableFuture<File> onBenchmarkStart(TownshipSchedulingBenchmarkRequest benchmarkRequest) {
        return schedulingService.benchmark(benchmarkRequest);
    }

}
