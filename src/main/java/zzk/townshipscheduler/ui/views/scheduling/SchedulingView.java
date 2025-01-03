package zzk.townshipscheduler.ui.views.scheduling;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
        List<SchedulingGameAction> schedulingGameActions = currentProblem.getSchedulingGameActions();
        Grid<SchedulingGameAction> grid = new Grid<>(SchedulingGameAction.class, false);
        grid.addColumn(SchedulingGameAction::getPlanningId).setHeader("#");
        grid.addColumn(SchedulingGameAction::getHumanReadable).setHeader("Description");
        grid.addColumn(SchedulingGameAction::getPlanningDateTimeSlot).setHeader("Planning Slot");
        grid.addComponentColumn(
                schedulingGameAction -> {
                    boolean boolProducing = schedulingGameAction instanceof SchedulingGameActionProductProducing;
                    if (boolProducing) {
                        SchedulingGameActionProductProducing productProducing = (SchedulingGameActionProductProducing) schedulingGameAction;
                        SchedulingProducingExecutionMode producingExecutionMode = productProducing.getPlanningProducingExecutionMode();
                        return new Text(producingExecutionMode != null ? producingExecutionMode.toString() : "N/A");
                    } else {
                        return new Text("N/A");
                    }
                }
        ).setHeader("Execution Mode(Producing Use)");
        grid.addComponentColumn(
                schedulingGameAction -> {
                    boolean boolProducing = schedulingGameAction instanceof SchedulingGameActionProductProducing;
                    if (boolProducing) {
                        SchedulingGameActionProductProducing productProducing = (SchedulingGameActionProductProducing) schedulingGameAction;
                        SchedulingFactoryInstance factoryInstance = productProducing.getPlanningFactoryInstance();
                        return new Text(factoryInstance != null ? factoryInstance.toString() : "N/A");
                    } else {
                        return new Text("N/A");
                    }
                }
        ).setHeader("Factory Instance(Producing Use)");
        grid.setItems(schedulingGameActions);

        Set<SchedulingOrder> orderSet = currentProblem.getSchedulingOrderSet();
        VerticalLayout orderSummarizeCard = new VerticalLayout();
        for (SchedulingOrder order : orderSet) {
            HorizontalLayout orderBasic = new HorizontalLayout();
            orderBasic.addClassNames(LumoUtility.Gap.SMALL);
            long id = order.getId();
            String orderType = order.getOrderType();
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

        schedulingViewWrapper.add(orderSummarizeCard);
        schedulingViewWrapper.addAndExpand(grid);
    }


}
