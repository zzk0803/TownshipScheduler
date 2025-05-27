package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.theme.lumo.LumoUtility;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.ui.pojo.SchedulingProducingArrangementVO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SchedulingReportArticle extends Composite<VerticalLayout> {

    private TownshipSchedulingProblem townshipSchedulingProblem;

    public SchedulingReportArticle(TownshipSchedulingProblem townshipSchedulingProblem) {
        this.townshipSchedulingProblem = townshipSchedulingProblem;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getContent().removeAll();

        if (townshipSchedulingProblem != null && townshipSchedulingProblem.getScore().isFeasible()) {
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
        var byDateTimeByFactoryByProductMapToCount
                = schedulingProducingArrangementList.stream()
                .map(SchedulingProducingArrangementVO::new)
                .collect(
                        Collectors.groupingBy(
                                SchedulingProducingArrangementVO::getArrangeDateTime,
                                TreeMap::new,
                                Collectors.groupingBy(
                                        SchedulingProducingArrangementVO::getArrangeFactory,
                                        Collectors.groupingBy(
                                                SchedulingProducingArrangementVO::getProduct,
                                                Collectors.counting()
                                        )
                                )
                        )
                );
        ByDateTimeByFactoryByProductMapToCountGrid grid = new ByDateTimeByFactoryByProductMapToCountGrid();
        grid.setItems(byDateTimeByFactoryByProductMapToCount.entrySet());
        grid.addComponentColumn(DateTimeFactoryArrangementsCard::new);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        getContent().addAndExpand(grid);
    }

    class ByDateTimeByFactoryByProductMapToCountGrid
            extends Grid<Map.Entry<LocalDateTime, Map<String, Map<String, Long>>>> {

    }

    class DateTimeFactoryArrangementsCard extends Composite<HorizontalLayout> {

        public DateTimeFactoryArrangementsCard(Map.Entry<LocalDateTime, Map<String, Map<String, Long>>> entry) {
            LocalDateTime arrangeDateTime = entry.getKey();
            Map<String, Map<String, Long>> factoryAndArrangements = entry.getValue();

            factoryAndArrangements.entrySet()
                    .stream()
                    .map(stringListEntry -> {
                        String factory = stringListEntry.getKey();
                        Element factoryHeader = ElementFactory.createHeading4(factory);

                        var productCountMap = stringListEntry.getValue();
                        Div div = new Div();
                        div.addClassNames(
                                LumoUtility.Width.AUTO,
                                LumoUtility.Gap.SMALL,
                                LumoUtility.Margin.Horizontal.XSMALL
                        );

                        div.add(productCountMap.entrySet()
                                .stream()
                                .map((productAmountEntry) -> String.format(
                                        "%s x%s",
                                        productAmountEntry.getKey(),
                                        productAmountEntry.getValue()
                                ))
                                .collect(Collectors.joining(",")));

                        Article article = new Article();
                        article.addClassNames(LumoUtility.Width.AUTO, LumoUtility.Height.AUTO);
                        article.getElement().appendChild(factoryHeader);
                        article.add(div);
                        return article;
                    })
                    .forEach(article -> {
                        getContent().add(article);
                    });

            Element dateTimeHeader = ElementFactory.createHeading4(arrangeDateTime.format(DateTimeFormatter.ofPattern("M-dd HH:mm")));
            getContent().getElement().insertChild(0, dateTimeHeader);
        }

        @Override
        protected HorizontalLayout initContent() {
            HorizontalLayout horizontalLayout = super.initContent();
            horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            horizontalLayout.setAlignItems(FlexComponent.Alignment.START);
            return horizontalLayout;
        }

    }

}
