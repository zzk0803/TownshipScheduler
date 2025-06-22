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
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.scheduling.model.DateTimeSlotSize;
import zzk.townshipscheduler.backend.scheduling.model.ProductAmountBill;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.ui.components.LitSchedulingVisTimelinePanel;
import zzk.townshipscheduler.ui.components.OrderGrid;
import zzk.townshipscheduler.ui.components.TriggerButton;
import zzk.townshipscheduler.ui.pojo.SchedulingProblemVo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Route("/scheduling/:schedulingId?")
@Menu(title = "Scheduling", order = 6.00d)
@PermitAll
@Setter
@Getter
public class SchedulingView extends VerticalLayout implements BeforeEnterObserver {

    private final SchedulingViewPresenter schedulingViewPresenter;

    private TriggerButton triggerButton;

    private Paragraph scoreAnalysisParagraph;

    private LitSchedulingVisTimelinePanel arrangementTimelinePanel;

    private Grid<SchedulingProducingArrangement> arrangementGrid;

    private TabSheet tabSheet;

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
        Optional<String> optionalSchedulingId = beforeEnterEvent.getRouteParameters().get("schedulingId");
        if (optionalSchedulingId.isPresent()) {
            if (this.schedulingViewPresenter.validProblemId(optionalSchedulingId.get())) {
                this.schedulingViewPresenter.setCurrentProblemId(optionalSchedulingId.get());
                removeAll();
                schedulingDetailUi();
            } else {
                ConfirmDialog confirmDialog = new ConfirmDialog(
                        "ERROR",
                        "scheduling not exist",
                        "OK",
                        confirmEvent -> {
                            UI.getCurrent().navigate(SchedulingView.class);
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
//        addAndExpand(buildGameActionTabSheetArticle());
        VerticalLayout schedulingContentLayout = new VerticalLayout();
        schedulingContentLayout.setSizeFull();
        addAndExpand(schedulingContentLayout);

        schedulingContentLayout.add(buildBtnPanel());
        tabSheet = new TabSheet();
        tabSheet.setWidthFull();
        tabSheet.add("Grid", buildProducingArrangementsGrid());
        tabSheet.add("Timeline", arrangementTimelinePanel = new LitSchedulingVisTimelinePanel(schedulingViewPresenter));
        schedulingContentLayout.addAndExpand(tabSheet);

    }

    private VerticalLayout buildProducingArrangementsGrid() {
        VerticalLayout gameActionArticle = new VerticalLayout();
        arrangementGrid = new Grid<>(SchedulingProducingArrangement.class, false);
        arrangementGrid.setMultiSort(true);
        arrangementGrid.addColumn(producingArrangement -> producingArrangement.getSchedulingProduct().getName())
                .setResizable(true)
                .setHeader("Product");
        arrangementGrid.addColumn(SchedulingProducingArrangement::getPlanningFactoryInstance)
                .setRenderer(new TextRenderer<>(producingArrangement -> {
                    return Optional.ofNullable(producingArrangement.getPlanningFactoryInstance())
                            .map(schedulingFactoryInstance -> schedulingFactoryInstance.getFactoryReadableIdentifier()
                                    .toString())
                            .orElse("N/A");
                }
                ))
                .setSortable(true)
                .setResizable(true)
                .setHeader("Assign Factory");
        arrangementGrid.addColumn(SchedulingProducingArrangement::getArrangeDateTime)
                .setRenderer(new LocalDateTimeRenderer<>(
                        SchedulingProducingArrangement::getArrangeDateTime,
                        "yyyy-MM-dd HH:mm:ss"
                ))
                .setSortable(true)
                .setResizable(true)
                .setHeader("Arrange Date Time");
        arrangementGrid.addColumn(SchedulingProducingArrangement::getProducingDateTime)
                .setRenderer(new LocalDateTimeRenderer<>(
                        SchedulingProducingArrangement::getProducingDateTime,
                        "yyyy-MM-dd HH:mm:ss"
                ))
                .setResizable(true)
                .setHeader("Producing Date Time");
        arrangementGrid.addColumn(SchedulingProducingArrangement::getCompletedDateTime)
                .setRenderer(new LocalDateTimeRenderer<>(
                        SchedulingProducingArrangement::getCompletedDateTime,
                        "yyyy-MM-dd HH:mm:ss"
                ))
                .setResizable(true)
                .setHeader("Completed Date Time");
        arrangementGrid.setSizeFull();
        schedulingViewPresenter.setupPlayerActionGrid(arrangementGrid);

        gameActionArticle.addAndExpand(arrangementGrid);
        return gameActionArticle;
    }

    private HorizontalLayout buildScorePanel() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        layout.setJustifyContentMode(JustifyContentMode.START);
        scoreAnalysisParagraph = new Paragraph();
        layout.add(new Details("Score Analysis:", scoreAnalysisParagraph));
        return layout;
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

            OrderGrid orderGrid = new OrderGrid(schedulingViewPresenter.allOrder(), false);
            orderGrid.setPageSize(4);
            orderGrid.setSelectionMode(Grid.SelectionMode.MULTI);
            orderGrid.asMultiSelect().select(orderGrid.getGenericDataView().getItems().toList());
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
            workCalendarEndPickerPicker.setValue(formDateTime.plus(Duration.ofMinutes(30)).plusDays(2));
            workCalendarEndPickerPicker.setMax(formDateTime.plusDays(9));
            schedulingForm.add(workCalendarStartPickerPicker, 1);
            schedulingForm.add(workCalendarEndPickerPicker, 1);

            dialogWrapper.add(schedulingForm);

            Dialog.DialogFooter footer = dialog.getFooter();
            footer.add(new Button(
                            "Start",
                            footerBtnClicked -> {
                                Set<OrderEntity> selectedOrder = orderGrid.getSelectedItems();
                                DateTimeSlotSize dateTimeSlotSize = slotSizeSelect.getValue();
                                LocalDateTime workCalendarStartPickerPickerValue = workCalendarStartPickerPicker.getValue();
                                LocalDateTime workCalendarEndPickerPickerValue = workCalendarEndPickerPicker.getValue();

                                String uuid = schedulingViewPresenter.backendPrepareTownshipScheduling(
                                        selectedOrder,
                                        dateTimeSlotSize,
                                        workCalendarStartPickerPickerValue,
                                        workCalendarEndPickerPickerValue
                                );

                                dialog.close();
                                UI.getCurrent().navigate(
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
        grid.addColumn(vo -> idRoller.getAndIncrement()).setHeader("#");
        grid.addColumn(new ComponentRenderer<>(
                        schedulingProblemVo -> new RouterLink(
                                schedulingProblemVo.getUuid(),
                                SchedulingView.class,
                                new RouteParameters("schedulingId", schedulingProblemVo.getUuid())
                        )))
                .setHeader("uuid");
        grid.addColumn(SchedulingProblemVo::getSolverStatus).setHeader("status");
        grid.setItems(schedulingViewPresenter.allSchedulingProblem());
        addAndExpand(grid);

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

    static class ReadonlyDateTimePicker extends DateTimePicker {

        private ReadonlyDateTimePicker(String label, LocalDateTime dateTime) {
            super(label, dateTime);
            setReadOnly(true);
        }

    }

    class ActionCard extends HorizontalLayout {

        private SchedulingProducingArrangement producingArrangement;

        public ActionCard(SchedulingProducingArrangement producingArrangement) {
            this();
            this.producingArrangement = producingArrangement;
            var planningFactory = producingArrangement.getPlanningFactoryInstance();
            LocalDateTime planningPlayerArrangeDateTime = producingArrangement.getArrangeDateTime();
            boolean scheduled = planningFactory != null && planningPlayerArrangeDateTime != null;

            VerticalLayout verticalLayout = new VerticalLayout();
            verticalLayout.add(
                    new Text(
                            "Type:" + (scheduled
                                    ? planningFactory.toString()
                                    : "N/A"
                            )
                    )
            );
            add(verticalLayout);

            String humanReadable = producingArrangement.getHumanReadable();
            add(
                    new VerticalLayout(
                            scheduled
                                    ? new ReadonlyDateTimePicker("Arrange", planningPlayerArrangeDateTime)
                                    : new Text("Arrange")
                    )
            );

            add(
                    new VerticalLayout(
                            scheduled
                                    ? new ReadonlyDateTimePicker(
                                    "Producing DateTime",
                                    producingArrangement.getProducingDateTime()
                            )
                                    : new Text("Producing")
                            ,
                            scheduled
                                    ? new ReadonlyDateTimePicker(
                                    "Completed DateTime",
                                    producingArrangement.getCompletedDateTime()
                            )
                                    : new Text("Completed")
                    )
            );
        }

        public ActionCard() {
            setHeight("15rem");
            setWidthFull();
            setDefaultVerticalComponentAlignment(Alignment.STRETCH);
            setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        }

        public SchedulingProducingArrangement getProducingArrangement() {
            return producingArrangement;
        }

        public void setProducingArrangement(SchedulingProducingArrangement producingArrangement) {
            this.producingArrangement = producingArrangement;
        }

    }

}
