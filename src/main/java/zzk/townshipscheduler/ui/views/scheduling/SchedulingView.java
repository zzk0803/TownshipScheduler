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
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.ui.components.TriggerButton;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Route("/scheduling")
@Menu
@PermitAll
@Setter
@Getter
public class SchedulingView extends VerticalLayout implements HasUrlParameter<String> {

    private final SchedulingViewPresenter schedulingViewPresenter;

    private UI ui;

    private Grid<AbstractPlayerProducingArrangement> actionGrid;

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
        actionGrid = new Grid<>(AbstractPlayerProducingArrangement.class, false);
        actionGrid.addColumn(AbstractPlayerProducingArrangement::getActionId)
                .setHeader("#")
                .setResizable(true)
                .setAutoWidth(true)
                .setFlexGrow(0);
        actionGrid.addColumn(factoryAction -> factoryAction.getSchedulingProduct().getName())
                .setHeader("Product")
                .setResizable(true)
                .setAutoWidth(true)
                .setFlexGrow(0);
        actionGrid.addComponentColumn(ActionCard::new)
                .setHeader("Factory/DateTime")
                .setSortable(true)
                .setComparator(
                        Comparator.comparing(AbstractPlayerProducingArrangement::getPlanningDateTimeSlotStartAsLocalDateTime)
                )
                .setFlexGrow(1)
                .setAutoWidth(true)
                .setResizable(true);
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


    class ActionCard extends HorizontalLayout {

        private AbstractPlayerProducingArrangement factoryAction;

        public ActionCard(AbstractPlayerProducingArrangement factoryAction) {
            this();
            this.factoryAction = factoryAction;
            AbstractFactoryInstance planningFactory = factoryAction.getFactory();
            LocalDateTime planningPlayerArrangeDateTime = factoryAction.getPlanningDateTimeSlotStartAsLocalDateTime();
            boolean scheduled = planningFactory != null && planningPlayerArrangeDateTime != null ;

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

            String humanReadable = factoryAction.getHumanReadable();
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
                                    factoryAction.getShadowGameProducingDateTime()
                            )
                                    : new Text("Producing")
                            ,
                            scheduled
                                    ? new ReadonlyDateTimePicker(
                                    "Completed DateTime",
                                    factoryAction.getShadowGameCompleteDateTime()
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

        public AbstractPlayerProducingArrangement getFactoryAction() {
            return factoryAction;
        }

        public void setFactoryAction(AbstractPlayerProducingArrangement factoryAction) {
            this.factoryAction = factoryAction;
        }

    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        schedulingViewPresenter.reset();
        actionGrid = null;
    }

    class ReadonlyDateTimePicker extends DateTimePicker {

        private ReadonlyDateTimePicker(String label, LocalDateTime dateTime) {
            super(label, dateTime);
            setReadOnly(true);
        }

    }

}
