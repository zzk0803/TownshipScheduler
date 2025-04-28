package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.ui.pojo.SchedulingProducingArrangementVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SchedulingReportArticle extends Composite<VerticalLayout> {

    private TownshipSchedulingProblem townshipSchedulingProblem;

    public SchedulingReportArticle() {

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getContent().removeAll();

        if (townshipSchedulingProblem != null && townshipSchedulingProblem.getScore().isSolutionInitialized()) {
            buildContentWithSolution(townshipSchedulingProblem);
        } else {
            buildEmptyContent();
        }
    }

    private void buildContentWithSolution(TownshipSchedulingProblem townshipSchedulingProblem) {
        List<SchedulingOrder> schedulingOrderList
                = townshipSchedulingProblem.getSchedulingOrderList();
        buildWithOrderContent(schedulingOrderList);

        List<SchedulingProducingArrangement> schedulingProducingArrangementList
                = townshipSchedulingProblem.getSchedulingProducingArrangementList();
        buildWithArrangementsContent(schedulingProducingArrangementList);
    }

    private void buildEmptyContent() {
        Div wrapperDiv = new Div();
        wrapperDiv.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.Height.FULL,
                LumoUtility.Width.FULL,
                LumoUtility.JustifyContent.CENTER,
                LumoUtility.AlignItems.CENTER
        );
        wrapperDiv.add(new H1("N/A"));
        getContent().add(wrapperDiv);
    }

    private void buildWithOrderContent(List<SchedulingOrder> schedulingOrderList) {

    }

    private void buildWithArrangementsContent(List<SchedulingProducingArrangement> schedulingProducingArrangementList) {
        Map<LocalDateTime, Map<String, List<SchedulingProducingArrangementVO>>> byDateTimeByFactoryMapToArrangements
                = schedulingProducingArrangementList.stream()
                .map(SchedulingProducingArrangementVO::new)
                .collect(
                        Collectors.groupingBy(
                                SchedulingProducingArrangementVO::getArrangeDateTime,
                                Collectors.groupingBy(SchedulingProducingArrangementVO::getArrangeFactory)
                        )
                );
        Set<Map.Entry<LocalDateTime, Map<String, List<SchedulingProducingArrangementVO>>>> entries
                = byDateTimeByFactoryMapToArrangements.entrySet();
        Grid<Map.Entry<LocalDateTime, Map<String, List<SchedulingProducingArrangementVO>>>> grid = new Grid<>();
        grid.setItems(entries);
        grid.addComponentColumn(DateTimeFactoryArrangementsCard::new);
        getContent().add(grid);
    }

    class DateTimeFactoryArrangementsCard extends Composite<HorizontalLayout> {

        public DateTimeFactoryArrangementsCard(Map.Entry<LocalDateTime, Map<String, List<SchedulingProducingArrangementVO>>> entry) {
            LocalDateTime arrangeDateTime = entry.getKey();
            Map<String, List<SchedulingProducingArrangementVO>> factoryAndArrangements = entry.getValue();
        }

    }

}
