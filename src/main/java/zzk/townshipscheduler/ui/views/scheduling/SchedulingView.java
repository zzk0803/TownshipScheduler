package zzk.townshipscheduler.ui.views.scheduling;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import zzk.townshipscheduler.backend.scheduling.ITownshipSchedulingService;
import zzk.townshipscheduler.backend.scheduling.ProductAmountBill;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Route("/scheduling")
@Menu
@PermitAll
public class SchedulingView extends VerticalLayout implements HasUrlParameter<String> {

    private final ITownshipSchedulingService schedulingService;

    private final VerticalLayout schedulingViewWrapper;

    private UUID currentProblemId;

    private TownshipSchedulingProblem currentProblem;

    public SchedulingView(
            ITownshipSchedulingService schedulingService
    ) {
        this.schedulingService = schedulingService;
        this.schedulingViewWrapper = new VerticalLayout();
        style();

        schedulingViewWrapper.add(new H1("Scheduling View"));
    }

    private void style() {
        schedulingViewWrapper.setSizeFull();
        addAndExpand(schedulingViewWrapper);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (
                Objects.nonNull(parameter)
                && !parameter.isBlank()
        ) {
            this.currentProblemId = UUID.fromString(parameter);
            this.currentProblem = this.schedulingService.getSchedule(this.currentProblemId);
            buildUI();
        } else {
            Notification.show("FIXME");
        }
    }

    private void buildUI() {
        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        Tab gameActionSheet = tabSheet.add("Game Action", buildGameActionTabSheetArticle());

        tabSheet.setSelectedTab(gameActionSheet);

        addAndExpand(tabSheet);
    }

    private VerticalLayout buildGameActionTabSheetArticle() {
        VerticalLayout gameActionArticle = new VerticalLayout();
        List<SchedulingPlayerFactoryAction> schedulingPlayerFactoryActions = currentProblem.getSchedulingPlayerFactoryActions();
        Grid<SchedulingPlayerFactoryAction> grid = new Grid<>(SchedulingPlayerFactoryAction.class, false);
        grid.addColumn(SchedulingPlayerFactoryAction::getActionId).setHeader("#");
        grid.addColumn(SchedulingPlayerFactoryAction::getHumanReadable).setHeader("Description");
        grid.addColumn(SchedulingPlayerFactoryAction::getPlanningPlayerDoItDateTime).setHeader("When To Do");
        grid.addComponentColumn(
                playerFactoryAction -> {
                    SchedulingGameActionExecutionMode producingExecutionMode = playerFactoryAction.getPlanningProducingExecutionMode();
                    return new Text(producingExecutionMode != null ? producingExecutionMode.toString() : "N/A");
                }
        ).setHeader("Execution Mode(Producing Use)");
        grid.addComponentColumn(
                playerFactoryAction -> {
                    SchedulingFactoryInstance planningFactory = playerFactoryAction.getPlanningFactory();
                    return new Text(Objects.toString(planningFactory, "N/A"));
                }
        ).setHeader("Factory Instance(Producing Use)");
        grid.setItems(schedulingPlayerFactoryActions);

        Set<SchedulingOrder> orderSet = currentProblem.getSchedulingOrderSet();

        gameActionArticle.add(buildOrderCard(orderSet));
        gameActionArticle.add(buildBtnPanel());
        gameActionArticle.addAndExpand(grid);
        return gameActionArticle;
    }

    private HorizontalLayout buildBtnPanel() {
        HorizontalLayout schedulingBtnPanel = new HorizontalLayout();
        Button stopBtn = new Button();
        stopBtn.setText("Stop");
        stopBtn.setVisible(false);
        stopBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_LARGE);
        Button startBtn = new Button();
        startBtn.setText("Start");
        startBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        startBtn.addClickListener(buttonClickEvent -> {
            UI.getCurrent().access(() -> {
                startBtn.setDisableOnClick(true);
                startBtn.setVisible(false);
                stopBtn.setVisible(true);
                schedulingService.scheduling(currentProblemId);
            });
        });
        stopBtn.addClickListener(buttonClickEvent -> {
            UI.getCurrent().access(() -> {
                stopBtn.setDisableOnClick(true);
                stopBtn.setVisible(false);
                startBtn.setVisible(true);
                schedulingService.abort(currentProblemId);
            });
        });
        schedulingBtnPanel.setWidthFull();
        schedulingBtnPanel.setJustifyContentMode(JustifyContentMode.END);
        schedulingBtnPanel.add(startBtn, stopBtn);
        return schedulingBtnPanel;
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
            bill.forEach((schedulingProduct, amount) -> orderItemAmounts.add(new VerticalLayout(
                    new Text(schedulingProduct.getName()),
                    new Text("X" + amount)
            )));

            orderSummarizeCard.add(orderBasic, orderItemAmounts);
        }
        return orderSummarizeCard;
    }


}
