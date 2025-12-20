package zzk.townshipscheduler.ui.views.scheduling;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.ui.components.LitSchedulingVisTimelinePanel;
import zzk.townshipscheduler.ui.components.OrderGrid;
import zzk.townshipscheduler.ui.components.SchedulingReportArticle;
import zzk.townshipscheduler.ui.components.TriggerButton;
import zzk.townshipscheduler.ui.pojo.SchedulingOrderVo;
import zzk.townshipscheduler.ui.pojo.SchedulingProblemVo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Route("/scheduling/:schedulingId?")
@PreserveOnRefresh
@Menu(title = "Scheduling", order = 6.00d)
@PermitAll
@Setter
@Getter
public class SchedulingView extends VerticalLayout implements BeforeEnterObserver {

    private final SchedulingViewPresenter schedulingViewPresenter;

    private TriggerButton triggerButton;

    private Paragraph scoreAnalysisParagraph;

    private LitSchedulingVisTimelinePanel arrangementTimelinePanel;

    private SchedulingReportArticle arrangementReportArticle;

    private TreeGrid<SchedulingProducingArrangement> arrangementTreeGrid;

    private TabSheet tabSheet;

    private Grid<SchedulingOrderVo> orderBriefGrid;

    private Paragraph briefText;

    public SchedulingView(
            SchedulingViewPresenter schedulingViewPresenter,
            TownshipAuthenticationContext townshipAuthenticationContext
    ) {
        this.schedulingViewPresenter = schedulingViewPresenter;
        this.schedulingViewPresenter.setSchedulingView(this);
        this.schedulingViewPresenter.setTownshipAuthenticationContext(townshipAuthenticationContext);
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
                removeAll();
                schedulingDetailUi();
            } else if (this.schedulingViewPresenter.checkWeatherProblemIsPersisted(problemId)) {
                this.schedulingViewPresenter.loadProblem(problemId);
                this.schedulingViewPresenter.setTownshipSchedulingProblemId(problemId);
                removeAll();
                schedulingDetailUi();
            } else {
                ConfirmDialog confirmDialog = new ConfirmDialog(
                        "ERROR",
                        "scheduling not exist",
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

        schedulingContentLayout.add(new Details("Order Brief", buildBriefPanel()));
        schedulingContentLayout.add(buildBtnPanel());
        tabSheet = new TabSheet();
        tabSheet.setWidthFull();
        tabSheet.add(
                "Grid",
                buildProducingArrangementsGrid()
        );
        tabSheet.add(
                "Timeline",
                arrangementTimelinePanel = new LitSchedulingVisTimelinePanel(schedulingViewPresenter)
        );
        tabSheet.add(
                "Report",
                arrangementReportArticle = new SchedulingReportArticle(
                        schedulingViewPresenter.findCurrentProblem(),
                        schedulingViewPresenter::getProductImage
                )
        );
        schedulingContentLayout.addAndExpand(tabSheet);

    }

    private VerticalLayout buildBriefPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.add(briefText = this.getSchedulingViewPresenter()
                .buildBriefText());

        FormLayout schedulingForm = new FormLayout();
        Select<DateTimeSlotSize> slotSizeSelect = new Select<>();
        slotSizeSelect.setLabel("Scheduling Time Slot");
        slotSizeSelect.setNoVerticalOverlap(true);
        slotSizeSelect.setItems(DateTimeSlotSize.values());
        this.getSchedulingViewPresenter()
                .setupSlotSizeSelectReadValue(slotSizeSelect);
        slotSizeSelect.setReadOnly(true);
        schedulingForm.add(slotSizeSelect, 2);

        DateTimePicker workCalendarStartPickerPicker = new DateTimePicker("Work Calendar Start");
        this.getSchedulingViewPresenter()
                .setupWorkCalendarStartPickerPickerReadValue(workCalendarStartPickerPicker);
        workCalendarStartPickerPicker.setReadOnly(true);
        DateTimePicker workCalendarEndPickerPicker = new DateTimePicker("Work Calendar End");
        this.getSchedulingViewPresenter()
                .setupWorkCalendarEndPickerPickerReadValue(workCalendarEndPickerPicker);
        workCalendarEndPickerPicker.setReadOnly(true);
        schedulingForm.add(workCalendarStartPickerPicker, 1);
        schedulingForm.add(workCalendarEndPickerPicker, 1);

        TimePicker playerSleepStartPicker = new TimePicker("Player Sleep Start");
        this.getSchedulingViewPresenter()
                .setupPlayerSleepStartPickerReadValue(playerSleepStartPicker);
        playerSleepStartPicker.setReadOnly(true);
        TimePicker playerSleepEndPicker = new TimePicker("Player Sleep End");
        this.getSchedulingViewPresenter()
                .setupPlayerSleepEndPickerReadValue(playerSleepEndPicker);
        playerSleepEndPicker.setReadOnly(true);
        schedulingForm.add(playerSleepStartPicker, 1);
        schedulingForm.add(playerSleepEndPicker, 1);
        panel.add(schedulingForm);

        orderBriefGrid = new Grid<>(SchedulingOrderVo.class, false);
        orderBriefGrid.addColumn(new ComponentRenderer<>(
                        schedulingOrderVo -> {
                            return new Span(schedulingOrderVo.getOrderType()
                                    .name() + "#" + schedulingOrderVo.getSerial());
                        }))
                .setHeader("Order Type # ID")
                .setAutoWidth(true)
                .setFlexGrow(0)
        ;
        orderBriefGrid.addColumn(new ComponentRenderer<>(funOrderBriefItemsRenderer()))
                .setHeader("Items")
                .setAutoWidth(true)
                .setFlexGrow(1)
        ;
        orderBriefGrid.addComponentColumn(schedulingOrderVo -> {
                    LocalDateTime deadline = schedulingOrderVo.getDeadline();
                    DateTimePicker dateTimePicker = new DateTimePicker(deadline);
                    dateTimePicker.setReadOnly(true);
                    return dateTimePicker;
                })
                .setHeader("Deadline")
                .setAutoWidth(true)
                .setFlexGrow(1)
        ;
        orderBriefGrid.addComponentColumn(schedulingOrderVo -> {
                    LocalDateTime deadline = schedulingOrderVo.getCompletedDateTime();
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
        this.getSchedulingViewPresenter()
                .setupOrderBriefGrid();
        return panel;
    }

    @NotNull
    private SerializableFunction<SchedulingOrderVo, Main> funOrderBriefItemsRenderer() {
        return schedulingOrderVo -> {
            Main layout = new Main();
            layout.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.ROW,
                    LumoUtility.Margin.NONE,
                    LumoUtility.Width.FULL,
                    LumoUtility.Height.FULL
            );
            ProductAmountBill productAmountBill = schedulingOrderVo.getProductAmountBill();
            Div div = new Div();
            div.addClassNames(
                    LumoUtility.Width.AUTO,
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.COLUMN
            );

            productAmountBill.entrySet()
                    .stream()
                    .map((productAmountEntry) -> {
                        Span span = new Span();
                        SchedulingProduct schedulingProduct = productAmountEntry.getKey();
                        String productName = schedulingProduct.getName();
                        span.add(this.schedulingViewPresenter.getProductImage(productName));
                        span.add(productName);
                        span.add(" x" + productAmountEntry.getValue());
                        return span;
                    })
                    .forEach(div::add)
            ;
            layout.add(div);
            return layout;
        };
    }

    private HorizontalLayout buildBtnPanel() {
        HorizontalLayout schedulingBtnPanel = new HorizontalLayout();
        Button startButon = new Button("Start");
        startButon.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        startButon.addClickListener(_ -> this.schedulingViewPresenter.onStartButton());
        Button stopButton = new Button("Stop");
        stopButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        stopButton.addClickListener(_ -> this.schedulingViewPresenter.onStopButton());
        this.triggerButton = new TriggerButton(startButon, stopButton);
        this.schedulingViewPresenter.setButtonState(this.triggerButton);
        schedulingBtnPanel.setWidthFull();
        schedulingBtnPanel.setJustifyContentMode(JustifyContentMode.BETWEEN);
        schedulingBtnPanel.add(buildScorePanel(), triggerButton);
        return schedulingBtnPanel;
    }

    private HorizontalLayout buildScorePanel() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        layout.setJustifyContentMode(JustifyContentMode.START);
        scoreAnalysisParagraph = new Paragraph();
        layout.add(scoreAnalysisParagraph);
        getSchedulingViewPresenter().setupScoreAnalysisParagraph();
        return layout;
    }

    private VerticalLayout buildProducingArrangementsGrid() {
        VerticalLayout gameActionArticle = new VerticalLayout();
        arrangementTreeGrid = new TreeGrid<>(SchedulingProducingArrangement.class, false);
        arrangementTreeGrid.setMultiSort(true);
        arrangementTreeGrid.addComponentHierarchyColumn(producingArrangement -> {
                    HorizontalLayout horizontalLayout = new HorizontalLayout();
                    horizontalLayout.setSpacing(false);
                    horizontalLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
                    String name = producingArrangement.getSchedulingProduct()
                            .getName();
                    horizontalLayout.add(this.getSchedulingViewPresenter()
                            .getProductImage(name));
                    horizontalLayout.add(name);
                    return horizontalLayout;
                })
                .setResizable(true)
                .setHeader("Product")
        ;
        arrangementTreeGrid.addColumn(SchedulingProducingArrangement::getSchedulingOrder)
                .setRenderer(new TextRenderer<>(schedulingProducingArrangement -> {
                    SchedulingOrder schedulingOrder = schedulingProducingArrangement.getSchedulingOrder();
                    return schedulingOrder.getOrderType() + "#" + schedulingOrder.getId();
                }))
                .setResizable(true)
                .setHeader("Order")
        ;
        arrangementTreeGrid.addColumn(SchedulingProducingArrangement::getPlanningFactoryInstance)
                .setRenderer(new TextRenderer<>(producingArrangement -> {
                    return Optional.ofNullable(producingArrangement.getPlanningFactoryInstance())
                            .map(schedulingFactoryInstance -> schedulingFactoryInstance.getFactoryReadableIdentifier()
                                    .toString())
                            .orElse("N/A");
                }
                ))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Assign Factory")
        ;
        arrangementTreeGrid.addColumn(SchedulingProducingArrangement::getStaticDeepProducingDuration)
                .setSortable(true)
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Static Producing Duration")
        ;
        arrangementTreeGrid.addColumn(SchedulingProducingArrangement::getArrangeDateTime)
                .setRenderer(new LocalDateTimeRenderer<>(
                        SchedulingProducingArrangement::getArrangeDateTime,
                        "yyyy-MM-dd HH:mm:ss"
                ))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setHeader("Arrange Date Time")
        ;
        arrangementTreeGrid.addColumn(SchedulingProducingArrangement::getProducingDateTime)
                .setRenderer(new LocalDateTimeRenderer<>(
                        SchedulingProducingArrangement::getProducingDateTime,
                        "yyyy-MM-dd HH:mm:ss"
                ))
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Producing Date Time")
        ;
        arrangementTreeGrid.addColumn(SchedulingProducingArrangement::getCompletedDateTime)
                .setRenderer(new LocalDateTimeRenderer<>(
                        SchedulingProducingArrangement::getCompletedDateTime,
                        "yyyy-MM-dd HH:mm:ss"
                ))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setHeader("Completed Date Time")
        ;

        arrangementTreeGrid.setSizeFull();

        schedulingViewPresenter.setupArrangementsTreeGrid(arrangementTreeGrid);

        gameActionArticle.addAndExpand(
                arrangementTreeGrid
        );
        return gameActionArticle;
    }

    private void schedulingOrdersUi() {
        Button newSchedulingBtn = new Button(VaadinIcon.PLUS.create());
        newSchedulingBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newSchedulingBtn.addClickListener(clicked -> {
            LocalDateTime formDateTime = LocalDateTime.now();

            Dialog dialog = new Dialog("Before Scheduler Start...");
            dialog.setSizeFull();

            VerticalLayout dialogWrapper = new VerticalLayout();
            dialogWrapper.setWidthFull();
            dialog.add(dialogWrapper);

            OrderGrid orderGrid = new OrderGrid(schedulingViewPresenter.fetchPlayerOrders(), false);
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
            schedulingForm.add(slotSizeSelect, 2);

            DateTimePicker workCalendarStartPickerPicker = new DateTimePicker("Work Calendar Start");
            workCalendarStartPickerPicker.setMin(formDateTime);
            workCalendarStartPickerPicker.setValue(formDateTime.plus(Duration.ofMinutes(30)));
            DateTimePicker workCalendarEndPickerPicker = new DateTimePicker("Work Calendar End");
            workCalendarEndPickerPicker.setMin(formDateTime);
            workCalendarEndPickerPicker.setValue(formDateTime.plus(Duration.ofMinutes(30))
                    .plusDays(2));
            workCalendarEndPickerPicker.setMax(formDateTime.plusDays(9));
            schedulingForm.add(workCalendarStartPickerPicker, 1);
            schedulingForm.add(workCalendarEndPickerPicker, 1);

            TimePicker playerSleepStartPicker = new TimePicker("Player Sleep Start");
            playerSleepStartPicker.setValue(SchedulingPlayer.DEFAULT_SLEEP_START);
            TimePicker playerSleepEndPicker = new TimePicker("Player Sleep End");
            playerSleepEndPicker.setValue(SchedulingPlayer.DEFAULT_SLEEP_END);
            schedulingForm.add(playerSleepStartPicker, 1);
            schedulingForm.add(playerSleepEndPicker, 1);

            dialogWrapper.add(schedulingForm);

            Dialog.DialogFooter footer = dialog.getFooter();
            footer.add(new Button(
                            "Confirm",
                            footerBtnClicked -> {
                                Set<OrderEntity> selectedOrder = orderGrid.getSelectedItems();
                                DateTimeSlotSize dateTimeSlotSize = slotSizeSelect.getValue();
                                LocalDateTime workCalendarStartPickerPickerValue =
                                        workCalendarStartPickerPicker.getValue();
                                LocalDateTime workCalendarEndPickerPickerValue = workCalendarEndPickerPicker.getValue();
                                LocalTime sleepStartPickerValue = playerSleepStartPicker.getValue();
                                LocalTime sleepEndPickerValue = playerSleepEndPicker.getValue();

                                String uuid = schedulingViewPresenter.backendPrepareTownshipScheduling(
                                        selectedOrder,
                                        dateTimeSlotSize,
                                        workCalendarStartPickerPickerValue,
                                        workCalendarEndPickerPickerValue,
                                        sleepStartPickerValue,
                                        sleepEndPickerValue
                                );

                                dialog.close();
                                UI.getCurrent()
                                        .navigate(
                                                SchedulingView.class,
                                                new RouteParam("schedulingId", uuid)
                                        );
                            }
                    )
            );

            dialog.open();
        });
        add(newSchedulingBtn);

        AtomicInteger idRoller = new AtomicInteger(1);
        Grid<SchedulingProblemVo> grid = new Grid<>(SchedulingProblemVo.class, false);
        grid.addColumn(vo -> idRoller.getAndIncrement())
                .setHeader("#")
                .setAutoWidth(true)
                .setFlexGrow(0)
        ;
        grid.addColumn(new ComponentRenderer<>(
                        schedulingProblemVo -> new RouterLink(
                                schedulingProblemVo.getUuid(),
                                SchedulingView.class,
                                new RouteParameters("schedulingId", schedulingProblemVo.getUuid())
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
        grid.addColumn(SchedulingProblemVo::getSolverStatus)
                .setHeader("status")
                .setAutoWidth(true)
                .setFlexGrow(1)
        ;
        grid.setItems(schedulingViewPresenter.viewFromLinkedSchedulingProblem());

        GridContextMenu<SchedulingProblemVo> problemGridContextMenu = grid.addContextMenu();
        problemGridContextMenu.addItem(
                "Unlink",
                clicked -> {
                    clicked.getItem()
                            .ifPresentOrElse(
                                    schedulingProblemVo -> {
                                        String problemId = schedulingProblemVo.getUuid();
                                        getSchedulingViewPresenter().getSchedulingService()
                                                .unlink(problemId)
                                        ;
                                        grid.setItems(schedulingViewPresenter.viewFromLinkedSchedulingProblem());
                                        Notification.show("Done");
                                    }, () -> {
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
                                        String problemId = schedulingProblemVo.getUuid();
                                        getSchedulingViewPresenter().getSchedulingService()
                                                .remove(problemId)
                                        ;
                                        grid.setItems(schedulingViewPresenter.viewFromLinkedSchedulingProblem());
                                        Notification.show("Done");
                                    }, () -> {
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
        addAndExpand(grid);

    }

    @NotNull
    private SerializableFunction<SchedulingProblemVo, Main> funOrdersGridItemsRenderer() {
        return schedulingProblemVo -> {
            Main layout = new Main();
            layout.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.ROW,
                    LumoUtility.Margin.NONE,
                    LumoUtility.Width.FULL,
                    LumoUtility.Height.FULL
            );
            List<SchedulingOrder> orderList = schedulingProblemVo.getOrderList();
            orderList.stream()
                    .map(schedulingOrder -> {
                        ProductAmountBill productAmountBill = schedulingOrder.getProductAmountBill();
                        Div div = new Div();
                        div.addClassNames(
                                LumoUtility.Width.AUTO,
                                LumoUtility.Display.FLEX,
                                LumoUtility.FlexDirection.COLUMN
                        );

                        div.add(new Span(schedulingOrder.getOrderType()
                                .name() + "#" + schedulingOrder.getId()));
                        productAmountBill.entrySet()
                                .stream()
                                .map((productAmountEntry) -> {
                                    Span span = new Span();
                                    SchedulingProduct schedulingProduct = productAmountEntry.getKey();
                                    String productName = schedulingProduct.getName();
                                    span.add(this.schedulingViewPresenter.getProductImage(productName));
                                    span.add(productName);
                                    span.add(" x" + productAmountEntry.getValue());
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

}
