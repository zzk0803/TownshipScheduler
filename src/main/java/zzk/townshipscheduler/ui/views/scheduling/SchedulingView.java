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
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.theme.lumo.LumoUtility;
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
import zzk.townshipscheduler.ui.pojo.*;

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
import java.util.zip.ZipOutputStream;

@Route("/scheduling/:schedulingId?")
@PreserveOnRefresh
@Menu(title = "Scheduling", order = 6.00d)
@PermitAll
@Setter
@Getter
public class SchedulingView
        extends VerticalLayout
        implements BeforeEnterObserver {

    private final SchedulingViewPresenter schedulingViewPresenter;

    private TriggerButton triggerButton;

    private Paragraph scoreAnalysisParagraph;

    private LitSchedulingVisTimelinePanel arrangementTimelinePanel;

    private SchedulingReportArticle arrangementReportArticle;

    private TreeGrid<SchedulingProducingArrangementViewModel> arrangementTreeGrid;

    private TabSheet tabSheet;

    private Grid<TownshipSchedulingProblemOrderBriefViewModel> orderBriefGrid;

    private Paragraph briefText;

    private ValueSignal<TownshipSchedulingProblemViewModel> townshipSchedulingProblemViewModelSignal
            = new ValueSignal<>(TownshipSchedulingProblemViewModel.EMPTY_NULL_VALUE);

    private Signal<Collection<SchedulingProducingArrangementViewModel>> SchedulingProducingArrangementsSignal
            = townshipSchedulingProblemViewModelSignal.map(townshipSchedulingProblemViewModel -> {
        if (townshipSchedulingProblemViewModel == null
                || TownshipSchedulingProblemViewModel.EMPTY_NULL_VALUE.equals(townshipSchedulingProblemViewModel)
        ) {
            return List.of();
        }
        return townshipSchedulingProblemViewModel.schedulingProducingArrangementViewModels();
    });

    private Signal<Collection<TownshipSchedulingProblemOrderBriefViewModel>> townshipSchedulingProblemOrderBriefSignal
            = townshipSchedulingProblemViewModelSignal.map(townshipSchedulingProblemViewModel -> {
        if (townshipSchedulingProblemViewModel == null
                || TownshipSchedulingProblemViewModel.EMPTY_NULL_VALUE.equals(townshipSchedulingProblemViewModel)
        ) {
            return List.of();
        }
        return townshipSchedulingProblemViewModel.toTownshipSchedulingProblemOrderBriefViewModels();

    });

    private Signal<SchedulingReportGroupsViewModel> schedulingReportGroupsViewModelSignal
            = townshipSchedulingProblemViewModelSignal.map(townshipSchedulingProblemViewModel -> {
        if (townshipSchedulingProblemViewModel == null
                || TownshipSchedulingProblemViewModel.EMPTY_NULL_VALUE.equals(townshipSchedulingProblemViewModel)
        ) {
            return SchedulingReportGroupsViewModel.EMPTY_NULL_VALUE;
        }
        return townshipSchedulingProblemViewModel.toSchedulingReportGroupsViewModel();
    });

    private Signal<String> scoreSignal = townshipSchedulingProblemViewModelSignal.map(townshipSchedulingProblemViewModel -> {
        if (townshipSchedulingProblemViewModel == null || TownshipSchedulingProblemViewModel.EMPTY_NULL_VALUE.equals(townshipSchedulingProblemViewModel)) {
            return "N/A";
        }
        return townshipSchedulingProblemViewModel.score();
    });

    private ValueSignal<Boolean> solverRunningSignal;

    public SchedulingView(
            TownshipAuthenticationContext townshipAuthenticationContext,
            SchedulingViewPresenter schedulingViewPresenter,
            TownshipSchedulingViewRecordComponent townshipSchedulingViewRecordComponent
    ) {
        this.schedulingViewPresenter = schedulingViewPresenter;
        this.schedulingViewPresenter.setSchedulingView(this);
        this.schedulingViewPresenter.setTownshipAuthenticationContext(townshipAuthenticationContext);
        this.schedulingViewPresenter.setTownshipSchedulingViewRecordComponent(townshipSchedulingViewRecordComponent);
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

        this.schedulingViewPresenter.signalTownshipSchedulingProblemViewModel();
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

        orderBriefGrid = new Grid<>(
                TownshipSchedulingProblemOrderBriefViewModel.class,
                false
        );
        orderBriefGrid.addColumn(new ComponentRenderer<>(
                        townshipSchedulingProblemOrderBriefViewModel -> {
                            return new Span(townshipSchedulingProblemOrderBriefViewModel.orderType() + "#" + townshipSchedulingProblemOrderBriefViewModel.id());
                        }))
                .setHeader("Order Type # ID")
                .setAutoWidth(true)
                .setFlexGrow(0)
        ;
        orderBriefGrid.addColumn(new ComponentRenderer<>(townshipSchedulingProblemOrderBriefViewModel1 -> {
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
        orderBriefGrid.addComponentColumn(townshipSchedulingProblemOrderBriefViewModel -> {
                    LocalDateTime deadline = townshipSchedulingProblemOrderBriefViewModel.deadline();
                    DateTimePicker dateTimePicker = new DateTimePicker(deadline);
                    dateTimePicker.setReadOnly(true);
                    return dateTimePicker;
                })
                .setHeader("Deadline")
                .setAutoWidth(true)
                .setFlexGrow(1)
        ;
        orderBriefGrid.addComponentColumn(townshipSchedulingProblemOrderBriefViewModel -> {
                    LocalDateTime deadline = townshipSchedulingProblemOrderBriefViewModel.calcCompletedDateTime();
                    if (Objects.nonNull(deadline)) {
                        DateTimePicker dateTimePicker = new DateTimePicker(deadline);
                        dateTimePicker.setReadOnly(true);
                        return dateTimePicker;
                    } else {
                        return new Text("N/A");
                    }
                })
                .setHeader("Completed Date Time")
                .setAutoWidth(true)
                .setFlexGrow(1)
        ;
        panel.addAndExpand(orderBriefGrid);

        Signal.effect(
                orderBriefGrid,
                () -> {
                    orderBriefGrid.setItems(this.townshipSchedulingProblemOrderBriefSignal.get());
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
            this.schedulingViewPresenter.onStartButton();
        });
        Button stopButton = new Button(stopButtonTimer);
        stopButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_LARGE);
        stopButtonTimer.setMode(LitTimer.Mode.COUNTUP);
        stopButton.setSuffixComponent(VaadinIcon.STOP.create());
        stopButton.addClickListener(_ -> {
            stopButtonTimer.pause();
            this.schedulingViewPresenter.onStopButton();
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
        scoreAnalysisParagraph.bindText(scoreSignal);
        layout.add(scoreAnalysisParagraph);
        return layout;
    }

    private VerticalLayout buildProducingArrangementsGrid() {
        VerticalLayout gameActionArticle = new VerticalLayout();
        arrangementTreeGrid = new TreeGrid<>(
                SchedulingProducingArrangementViewModel.class,
                false
        );
        arrangementTreeGrid.setMultiSort(true);
        arrangementTreeGrid.addComponentHierarchyColumn(producingArrangement -> {
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
        arrangementTreeGrid.addColumn(SchedulingProducingArrangementViewModel::order)
                .setRenderer(new TextRenderer<>(schedulingProducingArrangement -> {
                    return schedulingProducingArrangement.order()
                            .getReadable();
                }))
                .setResizable(true)
                .setHeader("Order")
        ;
        arrangementTreeGrid.addColumn(SchedulingProducingArrangementViewModel::assignedFactoryInstance)
                .setRenderer(new TextRenderer<>(producingArrangement -> {
                    return Optional.ofNullable(producingArrangement.assignedFactoryInstance())
                            .map(SchedulingFactoryInstanceViewModel::factoryReadableIdentifier)
                            .orElse("N/A");
                }
                ))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Assign Factory")
        ;
        arrangementTreeGrid.addColumn(SchedulingProducingArrangementViewModel::producingDuration)
                .setSortable(true)
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Item Producing Duration")
        ;
        arrangementTreeGrid.addColumn(SchedulingProducingArrangementViewModel::arrangeDateTime)
                .setRenderer(new LocalDateTimeRenderer<>(
                        SchedulingProducingArrangementViewModel::arrangeDateTime,
                        "yyyy-MM-dd HH:mm:ss"
                ))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setHeader("Arrange Date Time")
        ;
        arrangementTreeGrid.addColumn(SchedulingProducingArrangementViewModel::producingDateTime)
                .setRenderer(new LocalDateTimeRenderer<>(
                        SchedulingProducingArrangementViewModel::producingDateTime,
                        "yyyy-MM-dd HH:mm:ss"
                ))
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Producing Date Time")
        ;
        arrangementTreeGrid.addColumn(SchedulingProducingArrangementViewModel::completedDateTime)
                .setRenderer(new LocalDateTimeRenderer<>(
                        SchedulingProducingArrangementViewModel::completedDateTime,
                        "yyyy-MM-dd HH:mm:ss"
                ))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Completed Date Time")
        ;

        arrangementTreeGrid.setSizeFull();

        Signal.effect(
                arrangementTreeGrid,
                () -> {
                    Collection<SchedulingProducingArrangementViewModel> schedulingProducingArrangementViewModels
                            = SchedulingProducingArrangementsSignal.get();
                    TreeData<SchedulingProducingArrangementViewModel> arrangementTreeData = new TreeData<>();
                    arrangementTreeData.addItems(
                            schedulingProducingArrangementViewModels.stream()
                                    .filter(SchedulingProducingArrangementViewModel::boolDirectToOrder),
                            parentArrangement -> schedulingProducingArrangementViewModels.stream()
                                    .filter(parentArrangement::boolChild)
                    );
                    arrangementTreeGrid.setTreeData(arrangementTreeData);
                }
        );

        gameActionArticle.addAndExpand(
                arrangementTreeGrid
        );
        return gameActionArticle;
    }

    private void schedulingOrdersUi() {
        Button newSchedulingBtn = new Button(VaadinIcon.PLUS.create());
        newSchedulingBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newSchedulingBtn.addClickListener(clicked -> {
            Dialog dialog = new Dialog("Before Scheduler Start...");
            dialog.setSizeFull();

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
            slotSizeSelect.setValue(DateTimeSlotSize.HOUR);
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
                    text.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SECONDARY);
                    Optional<TownshipSchedulingProblemBriefViewModel> clickedItem = contextMenuItemClicked.getItem();
                    Dialog dialog = new Dialog();
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
                                                    dialogWrapper.remove(progressBar);
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

                                                        StreamResource resource = new StreamResource(
                                                                zipFileName,
                                                                () -> createZipInputStream(
                                                                        "BenchmarkReport",
                                                                        mayNullFile
                                                                )
                                                        );

                                                        Anchor downloadLink = new Anchor(
                                                                resource,
                                                                "Download Benchmark Repost(zip)"
                                                        );
                                                        downloadLink.getElement()
                                                                .setAttribute(
                                                                        "download",
                                                                        true
                                                                );
                                                        downloadLink.getElement()
                                                                .getThemeList()
                                                                .add("primary");

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
            this.benchmarkStrategy.setValue(TownshipSchedulingBenchmarkRequest.BenchmarkStrategy.BUILTIN);

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
