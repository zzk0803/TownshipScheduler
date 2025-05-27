package zzk.townshipscheduler.ui.views.scheduling;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.scheduling.model.ProductAmountBill;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.ui.components.LitSchedulingVisTimelinePanel;
import zzk.townshipscheduler.ui.components.TriggerButton;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Route("/scheduling")
@Menu
@PermitAll
@Setter
@Getter
public class SchedulingView extends VerticalLayout implements HasUrlParameter<String> {

    private final SchedulingViewPresenter schedulingViewPresenter;

    private UI ui;

    private TriggerButton triggerButton;

    private Paragraph scoreAnalysisParagraph;

    private LitSchedulingVisTimelinePanel arrangementTimelinePanel;

    private Grid<SchedulingProducingArrangement> arrangementGrid;

    private TabSheet tabSheet;

    public SchedulingView(SchedulingViewPresenter schedulingViewPresenter) {
        this.schedulingViewPresenter = schedulingViewPresenter;
        this.schedulingViewPresenter.setSchedulingView(this);
        this.setSizeFull();
        this.add(new H1("Scheduling View"));
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (
                Objects.nonNull(parameter)
                && !parameter.isBlank()
                && this.schedulingViewPresenter.validProblemId(parameter)
        ) {
            this.schedulingViewPresenter.setCurrentProblemId(parameter);
            buildUI();
        } else {
            Notification.show("FIXME");
        }
    }

    private void buildUI() {
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

    private HorizontalLayout buildBtnPanel() {
        HorizontalLayout schedulingBtnPanel = new HorizontalLayout();
        Button startButon = new Button("Start");
        startButon.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        startButon.addClickListener(_ -> this.schedulingViewPresenter.onStartButton());
        Button stopButton = new Button("Stop");
        stopButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        stopButton.addClickListener(_ -> this.schedulingViewPresenter.onStopButton());
        this.triggerButton = new TriggerButton(startButon, stopButton);
        schedulingBtnPanel.setWidthFull();
        schedulingBtnPanel.setJustifyContentMode(JustifyContentMode.BETWEEN);
        schedulingBtnPanel.add(buildScorePanel(), triggerButton);
        return schedulingBtnPanel;
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

    private VerticalLayout buildOrderCard(Set<SchedulingOrder> orderSet) {
        VerticalLayout orderSummarizeCard = new VerticalLayout();
        orderSummarizeCard.addClassNames(LumoUtility.Background.CONTRAST_20);
        for (SchedulingOrder order : orderSet) {
            HorizontalLayout orderBasic = new HorizontalLayout();
            long id = order.getId();
            String orderType = order.getOrderType().name();
            LocalDateTime deadline = order.getDeadline();
            orderBasic.add(new Text("Id:" + id), new Text("Type:" + orderType), new Text("Deadline:" + deadline));

            HorizontalLayout orderItemAmounts = new HorizontalLayout();
            ProductAmountBill bill = order.getProductAmountBill();
            bill.forEach(
                    (schedulingProduct, amount) -> orderItemAmounts.add(
                            new VerticalLayout(
                                    new Text(schedulingProduct.getName()),
                                    new Text("X" + amount)
                            ))
            );

            orderSummarizeCard.add(orderBasic, orderItemAmounts);
        }
        return orderSummarizeCard;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        this.schedulingViewPresenter.setUi(this.ui);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.schedulingViewPresenter.setUi(null);
        schedulingViewPresenter.reset();
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
