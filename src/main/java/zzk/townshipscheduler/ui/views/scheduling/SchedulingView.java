package zzk.townshipscheduler.ui.views.scheduling;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.arxila.javatuples.Pair;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.scheduling.TownshipSchedulingBenchmarkRequest;
import zzk.townshipscheduler.backend.scheduling.model.DateTimeSlotSize;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingPlayer;
import zzk.townshipscheduler.backend.utility.ReportZipUtil;
import zzk.townshipscheduler.ui.components.*;
import zzk.townshipscheduler.ui.pojo.scheduling.*;
import zzk.townshipscheduler.ui.pojo.scheduling.reactive.ReactiveSchedulingProducingArrangementViewModel;
import zzk.townshipscheduler.ui.pojo.scheduling.reactive.ReactiveTownshipSchedulingProblemOrderBriefViewModel;
import zzk.townshipscheduler.ui.pojo.scheduling.reactive.ReactiveTownshipSchedulingProblemViewModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@Route("/scheduling/:schedulingId?")
@PreserveOnRefresh
@Menu(
        title = "Scheduling",
        order = 6.00d
)
@PermitAll
@Setter
@Getter
public class SchedulingView
        extends VerticalLayout
        implements BeforeEnterObserver, BeforeLeaveObserver {

    private final SchedulingViewPresenter schedulingViewPresenter;

    private TriggerButton triggerButton;

    private Paragraph scoreAnalysisParagraph;

    private LitSchedulingVisTimelinePanel arrangementTimelinePanel;

    private SchedulingReportArticle arrangementReportArticle;

    private TreeGrid<ReactiveSchedulingProducingArrangementViewModel> reactiveArrangementTreeGrid;

    private TabSheet tabSheet;

    private Grid<ReactiveTownshipSchedulingProblemOrderBriefViewModel> reactiveOrderBriefGrid;

    private Paragraph briefText;

    private ValueSignal<Status> statusValueSignal = new ValueSignal<>(Status.EMPTY);

    private ValueSignal<ReactiveTownshipSchedulingProblemViewModel> reactiveTownshipSchedulingProblemViewModelValueSignal
            = new ValueSignal<>(ReactiveTownshipSchedulingProblemViewModel.EMPTY_NULL_VALUE);

    private Signal<Collection<ReactiveTownshipSchedulingProblemOrderBriefViewModel>> reactiveTownshipSchedulingProblemOrderBriefSignal
            = reactiveTownshipSchedulingProblemViewModelValueSignal.map(
            townshipSchedulingProblemViewModel -> {
                if (townshipSchedulingProblemViewModel == null
                    || TownshipSchedulingProblemViewModel.EMPTY_NULL_VALUE.equals(townshipSchedulingProblemViewModel)
                ) {
                    return List.of();
                }
                return townshipSchedulingProblemViewModel.toTownshipSchedulingProblemOrderBriefViewModels();

            });

    private Signal<Map<ReactiveTownshipSchedulingProblemOrderBriefViewModel, LocalDateTime>> townshipSchedulingProblemOrderBriefCompletedDateTimeSignal
            = reactiveTownshipSchedulingProblemOrderBriefSignal.map(
            townshipSchedulingProblemOrderBriefViewModels -> townshipSchedulingProblemOrderBriefViewModels != null
                    ? townshipSchedulingProblemOrderBriefViewModels.stream()
                    .filter(Objects::nonNull)
                    .map(reactiveTownshipSchedulingProblemOrderBriefViewModel -> {
                                LocalDateTime completedDateTime = reactiveTownshipSchedulingProblemOrderBriefViewModel.calcCompletedDateTime();
                                return completedDateTime != null
                                        ? new Pair<>(
                                        reactiveTownshipSchedulingProblemOrderBriefViewModel,
                                        completedDateTime
                                )
                                        : new Pair<>(
                                                reactiveTownshipSchedulingProblemOrderBriefViewModel,
                                                LocalDateTime.MAX
                                        );
                            }
                    )
                    .collect(Collectors.toMap(Pair::value0, Pair::value1))
                    : new HashMap<>()
    );

    private Signal<List<LitSchedulingProducingArrangementVO>> litSchedulingProducingArrangementVoListSignal
            = this.reactiveTownshipSchedulingProblemViewModelValueSignal
            .map(townshipSchedulingProblem -> {
                        if (townshipSchedulingProblem != null) {
                            return townshipSchedulingProblem.schedulingProducingArrangementReactiveViewModels().getValues()
                                    .map(schedulingProducingArrangement -> LitSchedulingProducingArrangementVO.of(schedulingProducingArrangement, true))
                                    .collect(Collectors.toCollection(ArrayList::new));
                        } else {
                            return List.of();
                        }
                    }
            );

    private Signal<SchedulingReportGroupsViewModel> schedulingReportGroupsViewModelSignal
            = this.reactiveTownshipSchedulingProblemViewModelValueSignal.map(
            townshipSchedulingProblemViewModel -> townshipSchedulingProblemViewModel != null
                    ? townshipSchedulingProblemViewModel.toSchedulingReportGroupsViewModel()
                    : SchedulingReportGroupsViewModel.EMPTY_NULL_VALUE
    );

    private Signal<String> solverResultSpanSignal = reactiveTownshipSchedulingProblemViewModelValueSignal.map(
            townshipSchedulingProblemViewModel -> {
                if (townshipSchedulingProblemViewModel != null) {
                    return townshipSchedulingProblemViewModel.feasible().get()
                            ? "Feasible"
                            : "Not Feasible";
                } else {
                    return "Not Feasible";
                }
            }
    );

    private ValueSignal<Boolean> solverRunningSignal = new ValueSignal<>(false);

    public SchedulingView(
            TownshipAuthenticationContext townshipAuthenticationContext,
            SchedulingViewPresenter schedulingViewPresenter,
            TownshipSchedulingProblemViewModelTransfer townshipSchedulingProblemViewModelTransfer
    ) {
        this.schedulingViewPresenter = schedulingViewPresenter;
        this.schedulingViewPresenter.setSchedulingView(this);
        this.schedulingViewPresenter.setTownshipAuthenticationContext(townshipAuthenticationContext);
        this.schedulingViewPresenter.setTownshipSchedulingViewRecordComponent(townshipSchedulingProblemViewModelTransfer);
        this.setSizeFull();
        this.add(new H1("Scheduling View"));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        this.schedulingViewPresenter.setUi(ui);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Optional<String> optionalSchedulingId = beforeEnterEvent.getRouteParameters()
                .get("schedulingId");
        if (optionalSchedulingId.isPresent()) {
            String problemId = optionalSchedulingId.get();
            if (this.schedulingViewPresenter.checkWeatherReadyToSolve(problemId)) {
                this.schedulingViewPresenter.setTownshipSchedulingProblemId(problemId);
                this.schedulingViewPresenter.reflushAndGetCurrentProblem();
                this.schedulingViewPresenter.reflushAndGetViewModel();
                removeAll();
                schedulingDetailUi();
            } else if (this.schedulingViewPresenter.checkWeatherProblemIsPersisted(problemId)) {
                this.schedulingViewPresenter.loadProblem(problemId);
                this.schedulingViewPresenter.setTownshipSchedulingProblemId(problemId);
                this.schedulingViewPresenter.reflushAndGetCurrentProblem();
                this.schedulingViewPresenter.reflushAndGetViewModel();
                removeAll();
                schedulingDetailUi();
            } else {
                ConfirmDialog confirmDialog = new ConfirmDialog(
                        "ERROR",
                        "schedule not exist",
                        "OK",
                        confirmEvent -> {
                            UI.getCurrent()
                                    .navigate(SchedulingView.class);
                        }
                );
                confirmDialog.open();
            }
        } else {
            removeAll();
            schedulingOrdersUi();
        }
    }

    private void schedulingDetailUi() {
        VerticalLayout schedulingContentLayout = new VerticalLayout();
        schedulingContentLayout.setSizeFull();
        addAndExpand(schedulingContentLayout);

        schedulingContentLayout.add(
                new Details(
                        "Order Brief",
                        buildBriefPanel()
                )
        );
        schedulingContentLayout.add(buildScoreAndButtonPanel());
        tabSheet = new TabSheet();
        tabSheet.setWidthFull();
        tabSheet.add(
                "Grid",
                buildProducingArrangementsGrid()
        );
        tabSheet.add(
                "Timeline",
                arrangementTimelinePanel = new LitSchedulingVisTimelinePanel(
                        this,
                        schedulingViewPresenter
                )
        );
        tabSheet.add(
                "Report",
                arrangementReportArticle = new SchedulingReportArticle(
                        this,
                        schedulingViewPresenter::getProductImage
                )
        );
        schedulingContentLayout.addAndExpand(tabSheet);

        this.schedulingViewPresenter.signalReactiveTownshipSchedulingProblemViewModel();
    }

    private VerticalLayout buildBriefPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.add(
                briefText = this.getSchedulingViewPresenter()
                        .buildBriefText()
        );

        FormLayout schedulingForm = new FormLayout();
        Select<DateTimeSlotSize> slotSizeSelect = new Select<>();
        slotSizeSelect.setLabel("Scheduling Time Slot");
        slotSizeSelect.setNoVerticalOverlap(true);
        slotSizeSelect.setItems(DateTimeSlotSize.values());
        this.getSchedulingViewPresenter()
                .setupSlotSizeSelectReadValue(slotSizeSelect);
        slotSizeSelect.setReadOnly(true);
        schedulingForm.add(
                slotSizeSelect,
                2
        );

        DateTimePicker workCalendarStartPickerPicker = new DateTimePicker("Work Calendar Start");
        this.getSchedulingViewPresenter()
                .setupWorkCalendarStartPickerPickerReadValue(workCalendarStartPickerPicker);
        workCalendarStartPickerPicker.setReadOnly(true);
        DateTimePicker workCalendarEndPickerPicker = new DateTimePicker("Work Calendar End");
        this.getSchedulingViewPresenter()
                .setupWorkCalendarEndPickerPickerReadValue(workCalendarEndPickerPicker);
        workCalendarEndPickerPicker.setReadOnly(true);
        schedulingForm.add(
                workCalendarStartPickerPicker,
                1
        );
        schedulingForm.add(
                workCalendarEndPickerPicker,
                1
        );

        TimePicker playerSleepStartPicker = new TimePicker("Player Sleep Start");
        this.getSchedulingViewPresenter()
                .setupPlayerSleepStartPickerReadValue(playerSleepStartPicker);
        playerSleepStartPicker.setReadOnly(true);
        TimePicker playerSleepEndPicker = new TimePicker("Player Sleep End");
        this.getSchedulingViewPresenter()
                .setupPlayerSleepEndPickerReadValue(playerSleepEndPicker);
        playerSleepEndPicker.setReadOnly(true);
        schedulingForm.add(
                playerSleepStartPicker,
                1
        );
        schedulingForm.add(
                playerSleepEndPicker,
                1
        );
        panel.add(schedulingForm);

        reactiveOrderBriefGrid = new Grid<>(
                ReactiveTownshipSchedulingProblemOrderBriefViewModel.class,
                false
        );
        reactiveOrderBriefGrid.addColumn(new ComponentRenderer<>(
                        townshipSchedulingProblemOrderBriefViewModel -> {
                            return new Span(townshipSchedulingProblemOrderBriefViewModel.orderType() + "#" + townshipSchedulingProblemOrderBriefViewModel.id());
                        }))
                .setHeader("Order Type # ID")
                .setAutoWidth(true)
                .setFlexGrow(0)
        ;
        reactiveOrderBriefGrid.addColumn(new ComponentRenderer<>(townshipSchedulingProblemOrderBriefViewModel1 -> {
                    Main layout = new Main();
                    layout.addClassNames(
                            LumoUtility.Display.FLEX,
                            LumoUtility.FlexDirection.ROW,
                            LumoUtility.Margin.NONE,
                            LumoUtility.Width.FULL,
                            LumoUtility.Height.FULL
                    );
                    ProductAmountBillViewModel productAmountBill = townshipSchedulingProblemOrderBriefViewModel1.productAmountBill();
                    Div div = new Div();
                    div.addClassNames(
                            LumoUtility.Width.AUTO,
                            LumoUtility.Display.FLEX,
                            LumoUtility.FlexDirection.COLUMN
                    );

                    productAmountBill.productAmountPairs()
                            .stream()
                            .map((schedulingProductAmountPair) -> {
                                Span span = new Span();
                                SchedulingProductViewModel schedulingProduct = schedulingProductAmountPair.product();
                                String productName = schedulingProduct.name();
                                span.add(this.schedulingViewPresenter.getProductImage(productName));
                                span.add(productName);
                                span.add(" x" + schedulingProductAmountPair.amount());
                                return span;
                            })
                            .forEach(div::add)
                    ;
                    layout.add(div);
                    return layout;
                }))
                .setHeader("Items")
                .setAutoWidth(true)
                .setFlexGrow(1)
        ;
        reactiveOrderBriefGrid.addComponentColumn(townshipSchedulingProblemOrderBriefViewModel -> {
                    LocalDateTime deadline = townshipSchedulingProblemOrderBriefViewModel.deadline();
                    DateTimePicker dateTimePicker = new DateTimePicker(deadline);
                    dateTimePicker.setReadOnly(true);
                    return dateTimePicker;
                })
                .setHeader("Deadline")
                .setAutoWidth(true)
                .setFlexGrow(1)
        ;
        reactiveOrderBriefGrid.addComponentColumn(
                        townshipSchedulingProblemOrderBriefViewModel -> {
                            DateTimePicker dateTimePicker = new DateTimePicker();
                            dateTimePicker.setReadOnly(true);
                            dateTimePicker.bindValue(
                                    townshipSchedulingProblemOrderBriefCompletedDateTimeSignal.map(
                                            computedSavedMap -> computedSavedMap != null
                                                    ? computedSavedMap.getOrDefault(townshipSchedulingProblemOrderBriefViewModel, LocalDateTime.MAX)
                                                    : LocalDateTime.MAX
                                    ),
                                    null
                            );
                            return dateTimePicker;
                        }
                )
                .setHeader("Completed Date Time")
                .setAutoWidth(true)
                .setFlexGrow(1)
        ;
        panel.addAndExpand(reactiveOrderBriefGrid);

        Signal.effect(
                reactiveOrderBriefGrid,
                () -> {
                    reactiveOrderBriefGrid.setItems(this.reactiveTownshipSchedulingProblemOrderBriefSignal.get());
                }
        );

        return panel;
    }

    private HorizontalLayout buildScoreAndButtonPanel() {
        HorizontalLayout schedulingBtnPanel = new HorizontalLayout();
        LitTimer stopButtonTimer = new LitTimer();
        stopButtonTimer.setHeight(1, Unit.REM);
        stopButtonTimer.setWidth(5, Unit.REM);
        Button startButon = new Button("Start");
        startButon.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_LARGE
        );
        startButon.setSuffixComponent(VaadinIcon.PLAY.create());
        startButon.addClickListener(_ -> {
            stopButtonTimer.reset();
            stopButtonTimer.start();
            this.schedulingViewPresenter.onSolverStartButton();
        });
        Button stopButton = new Button(stopButtonTimer);
        stopButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_LARGE);
        stopButtonTimer.setMode(LitTimer.Mode.COUNTUP);
        stopButton.setSuffixComponent(VaadinIcon.STOP.create());
        stopButton.addClickListener(_ -> {
            stopButtonTimer.pause();
            this.schedulingViewPresenter.onSolverStopButton();
        });
        this.triggerButton = new TriggerButton(
                startButon,
                stopButton
        );
        this.schedulingViewPresenter.setButtonState(this.triggerButton);
        schedulingBtnPanel.setWidthFull();
        schedulingBtnPanel.setJustifyContentMode(JustifyContentMode.BETWEEN);
        schedulingBtnPanel.add(
                buildScorePanel(),
                triggerButton
        );
        return schedulingBtnPanel;
    }

    private HorizontalLayout buildScorePanel() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        layout.setJustifyContentMode(JustifyContentMode.START);
        scoreAnalysisParagraph = new Paragraph();
        scoreAnalysisParagraph.bindText(
                Signal.computed(
                        () -> {
                            Status status = statusValueSignal.get();
                            switch (status) {
                                case EMPTY -> {
                                    return "N/A";
                                }

                                case READY -> {
                                    return "Ready To Start...";
                                }

                                case INIT -> {
                                    return "First Solution Initializing...";
                                }

                                case SOLVING, FINISHED -> {
                                    ReactiveTownshipSchedulingProblemViewModel reactiveTownshipSchedulingProblemViewModel = reactiveTownshipSchedulingProblemViewModelValueSignal.get();
                                    if (reactiveTownshipSchedulingProblemViewModel != null) {
                                        return reactiveTownshipSchedulingProblemViewModel.score().get();
                                    } else {
                                        return "N/A";
                                    }
                                }

                                case null, default -> {
                                    return "ERROR";
                                }
                            }
                        })
        );
        layout.add(scoreAnalysisParagraph);
        return layout;
    }

    private VerticalLayout buildProducingArrangementsGrid() {
        VerticalLayout gameActionArticle = new VerticalLayout();
        reactiveArrangementTreeGrid = new TreeGrid<>(
                ReactiveSchedulingProducingArrangementViewModel.class,
                false
        );
        reactiveArrangementTreeGrid.setMultiSort(true);
        reactiveArrangementTreeGrid.addComponentHierarchyColumn(producingArrangement -> {
                    HorizontalLayout horizontalLayout = new HorizontalLayout();
                    horizontalLayout.setSpacing(false);
                    horizontalLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
                    String name = producingArrangement.product()
                            .name();
                    horizontalLayout.add(this.getSchedulingViewPresenter()
                            .getProductImage(name));
                    horizontalLayout.add(name);
                    return horizontalLayout;
                })
                .setResizable(true)
                .setHeader("Product")
        ;

        reactiveArrangementTreeGrid.addColumn(ReactiveSchedulingProducingArrangementViewModel::producingDuration)
                .setSortable(true)
                .setResizable(true)
                .setHeader("Item Producing Duration")
        ;

        reactiveArrangementTreeGrid.addColumn(ReactiveSchedulingProducingArrangementViewModel::order)
                .setRenderer(new TextRenderer<>(schedulingProducingArrangement -> {
                    return schedulingProducingArrangement.order()
                            .getReadable();
                }))
                .setResizable(true)
                .setHeader("Order")
        ;

        reactiveArrangementTreeGrid.addColumn(ReactiveSchedulingProducingArrangementViewModel::factoryType)
                .setResizable(true)
                .setHeader("Product Factory Type");

        reactiveArrangementTreeGrid.addComponentColumn(
                        reactiveSchedulingProducingArrangementViewModel -> {
                            Span span = new Span();
                            span.bindText(reactiveSchedulingProducingArrangementViewModel.assignedFactoryInstance().map(schedulingFactoryInstanceViewModel -> schedulingFactoryInstanceViewModel != null
                                    ? schedulingFactoryInstanceViewModel.factoryReadableIdentifier()
                                    : "N/A"));
                            return span;
                        })
                .setSortable(true)
                .setComparator(Comparator.comparing(o -> o.assignedFactoryInstance().peek().factoryReadableIdentifier()))
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Assign Factory")
        ;

        reactiveArrangementTreeGrid.addComponentColumn(
                        reactiveSchedulingProducingArrangementViewModel -> {
                            Span span = new Span();
                            span.bindText(
                                    reactiveSchedulingProducingArrangementViewModel.arrangeDateTime()
                                            .map(schedulingFactoryInstanceViewModel -> schedulingFactoryInstanceViewModel != null
                                                    ? schedulingFactoryInstanceViewModel.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                                    : "N/A")
                            );
                            return span;
                        })
                .setSortable(true)
                .setComparator(Comparator.comparing(o -> o.arrangeDateTime().peek()))
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Arrange Date Time")
        ;
        reactiveArrangementTreeGrid.addComponentColumn(
                        reactiveSchedulingProducingArrangementViewModel -> {
                            Span span = new Span();
                            span.bindText(
                                    reactiveSchedulingProducingArrangementViewModel.producingDateTime()
                                            .map(schedulingFactoryInstanceViewModel -> schedulingFactoryInstanceViewModel != null
                                                    ? schedulingFactoryInstanceViewModel.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                                    : "N/A")
                            );
                            return span;
                        })
                .setSortable(true)
                .setComparator(Comparator.comparing(o -> o.producingDateTime().peek()))
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Producing Date Time")
        ;
        reactiveArrangementTreeGrid.addComponentColumn(
                        reactiveSchedulingProducingArrangementViewModel -> {
                            Span span = new Span();
                            span.bindText(
                                    reactiveSchedulingProducingArrangementViewModel.completedDateTime()
                                            .map(schedulingFactoryInstanceViewModel -> schedulingFactoryInstanceViewModel != null
                                                    ? schedulingFactoryInstanceViewModel.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                                    : "N/A")
                            );
                            return span;
                        })
                .setSortable(true)
                .setComparator(Comparator.comparing(o -> o.completedDateTime().peek()))
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Completed Date Time")
        ;

        reactiveArrangementTreeGrid.setSizeFull();

//        Signal.effect(
//                arrangementTreeGrid,
//                () -> {
//                    Collection<SchedulingProducingArrangementViewModel> schedulingProducingArrangementViewModels
//                            = SchedulingProducingArrangementsSignal.get();
//                    TreeData<SchedulingProducingArrangementViewModel> arrangementTreeData = new TreeData<>();
//                    arrangementTreeData.addItems(
//                            schedulingProducingArrangementViewModels.stream()
//                                    .filter(SchedulingProducingArrangementViewModel::boolDirectToOrder),
//                            parentArrangement -> schedulingProducingArrangementViewModels.stream()
//                                    .filter(parentArrangement::boolChild)
//                    );
//                    arrangementTreeGrid.setTreeData(arrangementTreeData);
//                }
//        );
        Signal.effect(
                reactiveArrangementTreeGrid,
                () -> {
                    TreeData<ReactiveSchedulingProducingArrangementViewModel> arrangementTreeData = new TreeData<>();
                    ListSignal<ReactiveSchedulingProducingArrangementViewModel> reactiveSchedulingProducingArrangementViewModelListSignal = reactiveTownshipSchedulingProblemViewModelValueSignal.get()
                            .schedulingProducingArrangementReactiveViewModels();
                    arrangementTreeData.addItems(
                            reactiveSchedulingProducingArrangementViewModelListSignal.getValues()
                                    .filter(ReactiveSchedulingProducingArrangementViewModel::boolDirectToOrder),
                            parentArrangement -> reactiveSchedulingProducingArrangementViewModelListSignal.getValues()
                                    .filter(parentArrangement::boolChild)
                    );
                    reactiveArrangementTreeGrid.setTreeData(arrangementTreeData);
                }
        );

        gameActionArticle.addAndExpand(reactiveArrangementTreeGrid);
        return gameActionArticle;
    }

    private void schedulingOrdersUi() {
        Button newSchedulingBtn = new Button(VaadinIcon.PLUS.create());
        newSchedulingBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newSchedulingBtn.addClickListener(clicked -> {
            Dialog dialog = new Dialog("Before Scheduler Start...");
            dialog.setWidth(67.8F, Unit.VW);

            VerticalLayout dialogWrapper = new VerticalLayout();
            dialogWrapper.setWidthFull();
            dialog.add(dialogWrapper);

            OrderGrid orderGrid = new OrderGrid(
                    schedulingViewPresenter.fetchPlayerOrders(),
                    false
            );
            orderGrid.setPageSize(4);
            orderGrid.setSelectionMode(Grid.SelectionMode.MULTI);
            orderGrid.asMultiSelect()
                    .select(orderGrid.getGenericDataView()
                            .getItems()
                            .toList());
            dialogWrapper.add(orderGrid);

            FormLayout schedulingForm = new FormLayout();

            Select<DateTimeSlotSize> slotSizeSelect = new Select<>();
            slotSizeSelect.setLabel("Scheduling Time Slot");
            slotSizeSelect.setNoVerticalOverlap(true);
            slotSizeSelect.setItems(DateTimeSlotSize.values());
            slotSizeSelect.setValue(DateTimeSlotSize.HALF_HOUR);
            schedulingForm.add(
                    slotSizeSelect,
                    2
            );

//            DateTimePicker workCalendarStartPickerPicker = new DateTimePicker("Work Calendar Start");
//            workCalendarStartPickerPicker.setMin(formDateTime);
//            workCalendarStartPickerPicker.setValue(formDateTime.plus(Duration.ofMinutes(30)));
//            DateTimePicker workCalendarEndPickerPicker = new DateTimePicker("Work Calendar End");
//            workCalendarEndPickerPicker.setMin(formDateTime);
//            workCalendarEndPickerPicker.setValue(formDateTime.plus(Duration.ofMinutes(30))
//                    .plusDays(2));
//            workCalendarEndPickerPicker.setMax(formDateTime.plusDays(9));
//            schedulingForm.add(workCalendarStartPickerPicker, 1);
//            schedulingForm.add(workCalendarEndPickerPicker, 1);

            TimePicker playerSleepStartPicker = new TimePicker("Player Sleep Start");
            playerSleepStartPicker.setValue(SchedulingPlayer.DEFAULT_SLEEP_START);
            TimePicker playerSleepEndPicker = new TimePicker("Player Sleep End");
            playerSleepEndPicker.setValue(SchedulingPlayer.DEFAULT_SLEEP_END);
            schedulingForm.add(
                    playerSleepStartPicker,
                    1
            );
            schedulingForm.add(
                    playerSleepEndPicker,
                    1
            );

            dialogWrapper.add(schedulingForm);

            Dialog.DialogFooter footer = dialog.getFooter();
            footer.add(
                    new Button(
                            "Confirm",
                            footerBtnClicked -> {
                                Set<OrderEntity> selectedOrder = orderGrid.getSelectedItems();
                                DateTimeSlotSize dateTimeSlotSize = slotSizeSelect.getValue();
//                                LocalDateTime workCalendarStartPickerPickerValue =
//                                        workCalendarStartPickerPicker.getValue();
//                                LocalDateTime workCalendarEndPickerPickerValue = workCalendarEndPickerPicker.getValue();
                                LocalTime sleepStartPickerValue = playerSleepStartPicker.getValue();
                                LocalTime sleepEndPickerValue = playerSleepEndPicker.getValue();

                                String uuid = schedulingViewPresenter.backendPrepareTownshipScheduling(
                                        selectedOrder,
                                        dateTimeSlotSize,
                                        LocalDateTime.now(),
//                                        LocalDateTime.now().plusDays(7),
                                        sleepStartPickerValue,
                                        sleepEndPickerValue
                                );

                                dialog.close();
                                UI.getCurrent()
                                        .navigate(
                                                SchedulingView.class,
                                                new RouteParam(
                                                        "schedulingId",
                                                        uuid
                                                )
                                        );
                            }
                    )
            );

            dialog.open();
        });
        add(newSchedulingBtn);

        AtomicInteger idRoller = new AtomicInteger(1);
        Grid<TownshipSchedulingProblemBriefViewModel> grid = new Grid<>(
                TownshipSchedulingProblemBriefViewModel.class,
                false
        );
        grid.addColumn(vo -> idRoller.getAndIncrement())
                .setHeader("#")
                .setAutoWidth(true)
                .setFlexGrow(0)
        ;
        grid.addColumn(new ComponentRenderer<>(
                        schedulingProblemVo -> new RouterLink(
                                schedulingProblemVo.uuid(),
                                SchedulingView.class,
                                new RouteParameters(
                                        "schedulingId",
                                        schedulingProblemVo.uuid()
                                )
                        )))
                .setHeader("UUID")
                .setAutoWidth(true)
                .setFlexGrow(1)
        ;
        grid.addColumn(new ComponentRenderer<>(funOrdersGridItemsRenderer()))
                .setHeader("Items")
                .setAutoWidth(true)
                .setFlexGrow(1)
        ;
        grid.addColumn(TownshipSchedulingProblemBriefViewModel::solverStatus)
                .setHeader("status")
                .setAutoWidth(true)
                .setFlexGrow(1)
        ;
        grid.setItems(schedulingViewPresenter.viewFromLinkedSchedulingProblem());

        GridContextMenu<TownshipSchedulingProblemBriefViewModel> problemGridContextMenu = grid.addContextMenu();
        problemGridContextMenu.addItem(
                "Unlink",
                clicked -> {
                    clicked.getItem()
                            .ifPresentOrElse(
                                    schedulingProblemVo -> {
                                        String problemId = schedulingProblemVo.uuid();
                                        getSchedulingViewPresenter().getSchedulingService()
                                                .unlink(problemId)
                                        ;
                                        grid.setItems(schedulingViewPresenter.viewFromLinkedSchedulingProblem());
                                        Notification.show("Done");
                                    },
                                    () -> {
                                        Notification.show("No Item");
                                    }
                            );
                }
        );
        problemGridContextMenu.addItem(
                "Remove",
                itemContentClicked -> {
                    itemContentClicked.getItem()
                            .ifPresentOrElse(
                                    schedulingProblemVo -> {
                                        String problemId = schedulingProblemVo.uuid();
                                        getSchedulingViewPresenter().getSchedulingService()
                                                .remove(problemId)
                                        ;
                                        grid.setItems(schedulingViewPresenter.viewFromLinkedSchedulingProblem());
                                        Notification.show("Done");
                                    },
                                    () -> {
                                        Notification.show("No Item");
                                    }
                            );

                }
        );
        problemGridContextMenu.addSeparator();
        problemGridContextMenu.addItem(
                "Load Settled Problems",
                clicked -> {
                    idRoller.set(1);
                    getSchedulingViewPresenter().getSchedulingService()
                            .loadPersistedSchedulingProblem()
                    ;
                    grid.setItems(schedulingViewPresenter.viewFromLinkedSchedulingProblem());
                    Notification.show("Done");
                }
        );
        problemGridContextMenu.addSeparator();
        setupBenchmarkFeature(problemGridContextMenu);
        addAndExpand(grid);

    }

    private SerializableFunction<TownshipSchedulingProblemBriefViewModel, Main> funOrdersGridItemsRenderer() {
        return schedulingProblemVo -> {
            Main layout = new Main();
            layout.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.ROW,
                    LumoUtility.Margin.NONE,
                    LumoUtility.Width.FULL,
                    LumoUtility.Height.FULL
            );
            Collection<SchedulingOrderViewModel> schedulingOrderViewModels = schedulingProblemVo.orderList();
            schedulingOrderViewModels.stream()
                    .map(schedulingOrder -> {
                        ProductAmountBillViewModel productAmountBill = schedulingOrder.productAmountBill();
                        Div div = new Div();
                        div.addClassNames(
                                LumoUtility.Width.AUTO,
                                LumoUtility.Display.FLEX,
                                LumoUtility.FlexDirection.COLUMN
                        );

                        div.add(new Span(schedulingOrder.orderType() + "#" + schedulingOrder.id()));
                        productAmountBill.productAmountPairs()
                                .stream()
                                .map((productAmountPair) -> {
                                    Span span = new Span();
                                    SchedulingProductViewModel schedulingProduct = productAmountPair.product();
                                    String productName = schedulingProduct.name();
                                    span.add(this.schedulingViewPresenter.getProductImage(productName));
                                    span.add(productName);
                                    span.add(" x" + productAmountPair.amount());
                                    return span;
                                })
                                .forEach(div::add)
                        ;
                        return div;
                    })
                    .forEachOrdered(layout::add)
            ;
            return layout;
        };
    }

    private void setupBenchmarkFeature(GridContextMenu<TownshipSchedulingProblemBriefViewModel> problemGridContextMenu) {
        problemGridContextMenu.addItem(
                "Benchmark",
                contextMenuItemClicked -> {
                    UI ui = UI.getCurrent();
                    Text text = new Text("Benchmark is RUNNING...");
                    Optional<TownshipSchedulingProblemBriefViewModel> clickedItem = contextMenuItemClicked.getItem();
                    Dialog dialog = new Dialog();
                    dialog.addClassNames("benchmark-dialog");
                    dialog.setModality(ModalityMode.STRICT);
                    dialog.setCloseOnEsc(false);
                    dialog.setCloseOnOutsideClick(false);
                    dialog.setWidth(
                            67.8F,
                            Unit.VW
                    );

                    Dialog.DialogHeader dialogHeader = dialog.getHeader();
                    HorizontalLayout dialogHeaderLayout = new HorizontalLayout();
                    dialogHeaderLayout.setAlignItems(Alignment.CENTER);
                    dialogHeaderLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
                    dialogHeaderLayout.setWidthFull();
                    dialogHeaderLayout.addComponentAsFirst(text);
                    dialogHeaderLayout.add(
                            new Button(VaadinIcon.CLOSE.create()) {{
                                addThemeVariants(ButtonVariant.WARNING);
                                addClickListener(dialogCloseClicked -> {
                                    dialog.close();
                                });
                            }}
                    );

                    dialogHeader.add(dialogHeaderLayout);

                    VerticalLayout dialogWrapper = new VerticalLayout();
                    dialogWrapper.setJustifyContentMode(JustifyContentMode.CENTER);
                    dialogWrapper.setAlignItems(Alignment.CENTER);
                    dialogWrapper.setSizeFull();
                    dialog.add(dialogWrapper);

                    clickedItem.ifPresent(schedulingProblemVo -> {
                        String problemUuid = schedulingProblemVo.uuid();

                        TownshipSchedulingBenchmarkRequestFormLayout form = new TownshipSchedulingBenchmarkRequestFormLayout(problemUuid);
                        Button startButton = new Button(VaadinIcon.PLAY_CIRCLE_O.create()) {{
                            addThemeVariants(
                                    ButtonVariant.LUMO_PRIMARY,
                                    ButtonVariant.LUMO_LARGE
                            );
                        }};
                        LitTimer timer = new LitTimer();
                        timer.getElement().setAttribute("theme", "large");
                        timer.setMode(LitTimer.Mode.COUNTUP);
                        dialogWrapper.addAndExpand(form);
                        dialogWrapper.add(startButton);
                        startButton.addClickListener(_ -> {
                            TownshipSchedulingBenchmarkRequest request = form.getTownshipSchedulingBenchmarkRequest();
                            form.frozen();
                            dialogWrapper.remove(startButton);
                            dialogWrapper.add(timer);
                            dialogWrapper.setHorizontalComponentAlignment(Alignment.CENTER, timer);

                            ProgressBar progressBar = new ProgressBar();
                            progressBar.setIndeterminate(true);
                            dialogWrapper.add(progressBar);

                            CompletableFuture<File> completableFuture = this.schedulingViewPresenter.onBenchmarkStart(request);
                            timer.start();
                            completableFuture.whenCompleteAsync(
                                    (mayNullFile, throwable) -> {
                                        ui.access(
                                                () -> {
                                                    text.setText("Benchmark finished.");
                                                    timer.pause();
                                                    Icon icon = VaadinIcon.CHECK_CIRCLE.create();
                                                    icon.setSize("5rem");
                                                    icon.getElement().getStyle().setColor("var(--lumo-success-color)");
                                                    dialogWrapper.replace(progressBar, icon);
                                                    if (throwable != null) {
                                                        dialogWrapper.add(new Paragraph(throwable.toString()));
                                                    }
                                                    if (mayNullFile != null) {

                                                        if (!mayNullFile.exists() || !mayNullFile.isDirectory()) {
                                                            dialogWrapper.add(new Span("file not exist?!"));
                                                            return;
                                                        }

                                                        String timestamp = LocalDateTime.now()
                                                                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                                                        String zipFileName = "Report_" + timestamp + ".zip";

//                                                        StreamResource resource = new StreamResource(
//                                                                zipFileName,
//                                                                () -> createZipInputStream(
//                                                                        "BenchmarkReport",
//                                                                        mayNullFile
//                                                                )
//                                                        );

                                                        Anchor downloadLink = new Anchor(
                                                                DownloadHandler.fromInputStream(downloadEvent -> {
                                                                    return new DownloadResponse(
                                                                            createZipInputStream(
                                                                                    "BenchmarkReport",
                                                                                    mayNullFile
                                                                            ),
                                                                            zipFileName,
                                                                            "octet-stream",
                                                                            -1
                                                                    );
                                                                }),
                                                                "Download Benchmark Report(zip)"
                                                        );
                                                        downloadLink.getElement()
                                                                .setAttribute(
                                                                        "download",
                                                                        true
                                                                );
                                                        downloadLink.getElement()
                                                                .getThemeList()
                                                                .add("primary");
                                                        downloadLink.addClassNames("benchmark-result-link");

                                                        dialogWrapper.add(downloadLink);
                                                        dialogWrapper.setHorizontalComponentAlignment(
                                                                Alignment.CENTER,
                                                                downloadLink
                                                        );

                                                    } else {
                                                        Notification notification = new Notification("couldn't find benchmark file(s)");
                                                        notification.addThemeVariants(NotificationVariant.ERROR);
                                                        notification.setPosition(Notification.Position.MIDDLE);
                                                        notification.setDuration(2);
                                                        notification.open();
                                                        dialog.close();
                                                    }
                                                }
                                        );
                                    },
                                    VaadinService.getCurrent()
                                            .getExecutor()
                            );

                        });

                    });
                    dialog.open();
                }
        );
    }

    private InputStream createZipInputStream(
            String zipRootName,
            File file
    ) {
        try {
            Path tempZip = Files.createTempFile(
                    "benchmark_report_",
                    ".zip"
            );

            try (var zos = new ZipOutputStream(Files.newOutputStream(tempZip))) {
                ReportZipUtil.zipReportDirectory(
                        zos,
                        file,
                        zipRootName
                );
            }

            return Files.newInputStream(
                    tempZip,
                    StandardOpenOption.DELETE_ON_CLOSE
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "benchmark report zip failed",
                    e
            );
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        Status status = getStatusValueSignal().peek();
        if (status == Status.INIT || status == Status.SOLVING) {
            BeforeLeaveEvent.ContinueNavigationAction navigationAction = event.postpone();
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader("Leaving Before Solver Finished...");
            confirmDialog.setText("Solver Task Shall Still Remain Running You Leaving...But Advice Stay For Prevent Passible Malfunction");
            confirmDialog.setCancelable(true);
            confirmDialog.addConfirmListener(_ -> navigationAction.proceed());
            confirmDialog.addCancelListener(_ -> navigationAction.cancel());
            confirmDialog.open();
        }
    }

    public enum Status {
        EMPTY, READY, INIT, SOLVING, FINISHED
    }

    public static class TownshipSchedulingBenchmarkRequestFormLayout
            extends Composite<VerticalLayout> {

        String problemUuid;

        TextField problemId = new TextField("Problem Id");

        Select<TownshipSchedulingBenchmarkRequest.BenchmarkSize> benchmarkSize = new Select<>("Benchmark Problem Size");

        Select<TownshipSchedulingBenchmarkRequest.BenchmarkStrategy> benchmarkStrategy = new Select<>("Benchmark Strategy");

        @Getter
        TownshipSchedulingBenchmarkRequest townshipSchedulingBenchmarkRequest = new TownshipSchedulingBenchmarkRequest();

        Binder<TownshipSchedulingBenchmarkRequest> binder = new Binder<>(TownshipSchedulingBenchmarkRequest.class);

        public TownshipSchedulingBenchmarkRequestFormLayout(String problemUuid) {
            this.problemUuid = problemUuid;
            this.townshipSchedulingBenchmarkRequest.setProblemId(this.problemUuid);
            this.binder.bindReadOnly(
                    this.problemId,
                    TownshipSchedulingBenchmarkRequest::getProblemId
            );
            this.binder.forField(benchmarkSize)
                    .bind(
                            TownshipSchedulingBenchmarkRequest::getBenchmarkSize,
                            TownshipSchedulingBenchmarkRequest::setBenchmarkSize
                    );
            this.binder.forField(benchmarkStrategy)
                    .bind(
                            TownshipSchedulingBenchmarkRequest::getBenchmarkStrategy,
                            TownshipSchedulingBenchmarkRequest::setBenchmarkStrategy
                    );
            this.binder.setBean(townshipSchedulingBenchmarkRequest);

            this.benchmarkSize.setItems(TownshipSchedulingBenchmarkRequest.BenchmarkSize.values());
            this.benchmarkStrategy.setItems(TownshipSchedulingBenchmarkRequest.BenchmarkStrategy.values());

            this.benchmarkSize.setValue(TownshipSchedulingBenchmarkRequest.BenchmarkSize.SELF);
            this.benchmarkStrategy.setValue(TownshipSchedulingBenchmarkRequest.BenchmarkStrategy.NIGHTLY_RESEARCH);

            getContent().add(
                    problemId,
                    benchmarkSize,
                    benchmarkStrategy
            );
        }

        public void frozen() {
            this.benchmarkSize.setReadOnly(true);
            this.benchmarkStrategy.setReadOnly(true);
        }

        @Override
        protected VerticalLayout initContent() {
            VerticalLayout verticalLayout = super.initContent();
            verticalLayout.setJustifyContentMode(JustifyContentMode.CENTER);
            verticalLayout.setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
            return verticalLayout;
        }

    }

    public static class SchedulingProcessingStartComponentEvent
            extends ComponentEvent<SchedulingView> {

        private String message;

        public SchedulingProcessingStartComponentEvent(
                SchedulingView source,
                boolean fromClient,
                String message
        ) {
            this(
                    source,
                    fromClient
            );
            this.message = message;
        }

        public SchedulingProcessingStartComponentEvent(
                SchedulingView source,
                boolean fromClient
        ) {
            super(
                    source,
                    fromClient
            );
        }

    }

    public static class SchedulingProcessingEndComponentEvent
            extends ComponentEvent<SchedulingView> {

        private String message;

        public SchedulingProcessingEndComponentEvent(
                SchedulingView source,
                boolean fromClient,
                String message
        ) {
            this(
                    source,
                    fromClient
            );
            this.message = message;
        }

        public SchedulingProcessingEndComponentEvent(
                SchedulingView source,
                boolean fromClient
        ) {
            super(
                    source,
                    fromClient
            );
        }

    }

}
