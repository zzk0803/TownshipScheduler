package zzk.townshipscheduler.ui.views.scheduling;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.scheduling.model.*;
import zzk.townshipscheduler.ui.components.TriggerButton;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Route("/scheduling")
@Menu
@PermitAll
@PreserveOnRefresh
@Setter
@Getter
public class SchedulingView extends VerticalLayout implements HasUrlParameter<String> {

    private final SchedulingViewPresenter schedulingViewPresenter;

    private UI ui;

    private Grid<SchedulingPlayerFactoryAction> actionGrid;

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
            this.schedulingViewPresenter.setCurrentProblemId(UUID.fromString(parameter));
            buildUI();
        } else {
            Notification.show("FIXME");
        }
    }

    private void buildUI() {
        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        Tab tab = tabSheet.add(
                "Game Action",
                buildGameActionTabSheetArticle()
        );

        tabSheet.setSelectedTab(tab);

        addAndExpand(tabSheet);
    }

    private VerticalLayout buildGameActionTabSheetArticle() {
        VerticalLayout gameActionArticle = new VerticalLayout();
        actionGrid = new Grid<>(SchedulingPlayerFactoryAction.class, false);
        actionGrid.addColumn(SchedulingPlayerFactoryAction::getActionId)
                .setHeader("#")
                .setResizable(true);
        actionGrid.addColumn(SchedulingPlayerFactoryAction::getHumanReadable)
                .setHeader("Description")
                .setResizable(true);
//        actionGrid.addComponentColumn(
//                        playerFactoryAction -> {
//                            SchedulingProducingExecutionMode producingExecutionMode = playerFactoryAction.getProducingExecutionMode();
//                            return new Text(producingExecutionMode != null ? producingExecutionMode.toString() : "N/A");
//                        }
//                ).setHeader("Execution Mode(Producing Use)")
//                .setResizable(true);
        actionGrid.addComponentColumn(
                playerFactoryAction -> {
                    SchedulingFactoryTimeSlotInstance planningFactory = playerFactoryAction.getPlanningTimeSlotFactory();
                    return new Text(Objects.toString(planningFactory, "N/A"));
                }
        ).setHeader("Factory/DateTime").setResizable(true);

        schedulingViewPresenter.setupPlayerActionGrid(actionGrid);

        gameActionArticle.add(buildOrderCard(this.schedulingViewPresenter.getSchedulingOrder()));
        gameActionArticle.add(buildBtnPanel());
        gameActionArticle.addAndExpand(actionGrid);
        return gameActionArticle;
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
        schedulingBtnPanel.setJustifyContentMode(JustifyContentMode.AROUND);
        schedulingBtnPanel.add(buildScorePanel(),triggerButton);
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


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        this.schedulingViewPresenter.setUi(this.ui);
    }

}
