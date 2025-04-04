package zzk.townshipscheduler.ui.views.scheduling;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.scheduling.model.BaseProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.ProductAmountBill;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.ui.components.TriggerButton;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Route("/scheduling")
@Menu
@PermitAll
@Setter
@Getter
public class SchedulingView extends VerticalLayout implements HasUrlParameter<String> {

    private final SchedulingViewPresenter schedulingViewPresenter;

    private UI ui;

    private Grid<BaseProducingArrangement> actionGrid;

    private Paragraph scoreAnalysisParagraph;

    public SchedulingView(
            SchedulingViewPresenter schedulingViewPresenter
    ) {
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
        addAndExpand(buildGameActionTabSheetArticle());
    }

    private VerticalLayout buildGameActionTabSheetArticle() {
        VerticalLayout gameActionArticle = new VerticalLayout();
        actionGrid = new Grid<>(BaseProducingArrangement.class,false);
        actionGrid.addColumn(BaseProducingArrangement::getSchedulingProduct)
                .setResizable(true)
                .setHeader("Product");
        actionGrid.addColumn(BaseProducingArrangement::getPlanningFactoryInstance)
                .setResizable(true)
                .setHeader("Assign Factory");
        actionGrid.addColumn(BaseProducingArrangement::getPlanningDateTimeSlotStartAsLocalDateTime)
                .setResizable(true)
                .setHeader("Arrange Date Time");
        actionGrid.addColumn(BaseProducingArrangement::getProducingDateTime)
                .setResizable(true)
                .setHeader("Producing Date Time");
        actionGrid.addColumn(BaseProducingArrangement::getCompletedDateTime)
                .setResizable(true)
                .setHeader("Completed Date Time");
        actionGrid.setSizeFull();
        schedulingViewPresenter.setupPlayerActionGrid(actionGrid);

//        gameActionArticle.add(buildOrderCard(this.schedulingViewPresenter.getSchedulingOrder()));
        gameActionArticle.add(buildBtnPanel());
        gameActionArticle.addAndExpand(actionGrid);
        return gameActionArticle;
    }

    private HorizontalLayout buildBtnPanel() {
        HorizontalLayout schedulingBtnPanel = new HorizontalLayout();
        TriggerButton triggerButton = new TriggerButton(
                "Start",
                buttonClickEvent -> {
                    this.schedulingViewPresenter.schedulingAndPush();
                },
                "Stop",
                buttonClickEvent1 -> {
                    this.schedulingViewPresenter.schedulingAbort();
                }
        );
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
        schedulingViewPresenter.reset();
        actionGrid = null;
    }

    class ActionCard extends HorizontalLayout {

        private BaseProducingArrangement producingArrangement;

        public ActionCard(BaseProducingArrangement producingArrangement) {
            this();
            this.producingArrangement = producingArrangement;
            var planningFactory = producingArrangement.getPlanningFactoryInstance();
            LocalDateTime planningPlayerArrangeDateTime = producingArrangement.getPlanningDateTimeSlotStartAsLocalDateTime();
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

        public BaseProducingArrangement getProducingArrangement() {
            return producingArrangement;
        }

        public void setProducingArrangement(BaseProducingArrangement producingArrangement) {
            this.producingArrangement = producingArrangement;
        }

    }

    class ReadonlyDateTimePicker extends DateTimePicker {

        private ReadonlyDateTimePicker(String label, LocalDateTime dateTime) {
            super(label, dateTime);
            setReadOnly(true);
        }

    }

}
