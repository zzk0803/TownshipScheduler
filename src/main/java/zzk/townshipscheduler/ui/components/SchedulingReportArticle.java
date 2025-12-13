package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.scheduling.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
public class SchedulingReportArticle extends Composite<VerticalLayout> {

    private TownshipSchedulingProblem townshipSchedulingProblem;

    private Function<String, Image> fetchImgByIdProvider;

    public SchedulingReportArticle(TownshipSchedulingProblem townshipSchedulingProblem) {
        this.townshipSchedulingProblem = townshipSchedulingProblem;
    }

    public SchedulingReportArticle(
            TownshipSchedulingProblem townshipSchedulingProblem,
            Function<String, Image> fetchImgByIdProvider
    ) {
        this.townshipSchedulingProblem = townshipSchedulingProblem;
        this.fetchImgByIdProvider = fetchImgByIdProvider;
    }

    public void update(TownshipSchedulingProblem townshipSchedulingProblem) {
        this.townshipSchedulingProblem = townshipSchedulingProblem;
        update();
    }

    private void update() {
        getContent().removeAll();

        if (townshipSchedulingProblem != null) {
            buildContentWithSolution(townshipSchedulingProblem);
        } else {
            buildEmptyContent();
        }
    }

    private void buildContentWithSolution(TownshipSchedulingProblem townshipSchedulingProblem) {
        List<SchedulingProducingArrangement> schedulingProducingArrangementList
                = townshipSchedulingProblem.getSchedulingProducingArrangementList();
        buildWithArrangementsContent(schedulingProducingArrangementList);
    }

    private void buildWithArrangementsContent(List<SchedulingProducingArrangement> schedulingProducingArrangementList) {
        var byDateTimeByFactoryByProductMapToCount
                = schedulingProducingArrangementList.stream()
                .collect(
                        Collectors.groupingBy(
                                SchedulingProducingArrangement::getArrangeDateTime,
                                TreeMap::new,
                                Collectors.groupingBy(
                                        SchedulingProducingArrangement::getPlanningFactoryInstance,
                                        Collectors.groupingBy(
                                                SchedulingProducingArrangement::getSchedulingProduct,
                                                Collectors.counting()
                                        )
                                )
                        )
                );
        ByDateTimeByFactoryByProductMapToCountGrid grid = new ByDateTimeByFactoryByProductMapToCountGrid();
        grid.setItems(byDateTimeByFactoryByProductMapToCount.entrySet());
        grid.addComponentColumn(DateTimeFactoryArrangementsCard::new);
        addErrorSpanIfNotFeasible();
        getContent().addAndExpand(grid);
    }

    private void addErrorSpanIfNotFeasible() {
        if (getTownshipSchedulingProblem().getScore().isFeasible()) {
            Span span = new Span("Eureka");
            span.getElement().getThemeList().add("badge success");
            getContent().add(span);
        } else {
            Span span = new Span("Not Feasible");
            span.getElement().getThemeList().add("badge contrast error");
            getContent().add(span);
        }
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        update();
    }

    private Image getProductImage(String productName) {
        return this.fetchImgByIdProvider.apply(productName);
    }


    class ByDateTimeByFactoryByProductMapToCountGrid
            extends Grid<Map.Entry<LocalDateTime, Map<SchedulingFactoryInstance, Map<SchedulingProduct, Long>>>> {

    }

    class DateTimeFactoryArrangementsCard extends Composite<HorizontalLayout> {

        public DateTimeFactoryArrangementsCard(
                Map.Entry<LocalDateTime, Map<SchedulingFactoryInstance, Map<SchedulingProduct, Long>>> entry
        ) {
            LocalDateTime arrangeDateTime = entry.getKey();
            var factoryAndArrangements = entry.getValue();

            buildItemsContent(factoryAndArrangements);
            buildDateTimeContent(arrangeDateTime);
        }

        private void buildDateTimeContent(LocalDateTime arrangeDateTime) {
            Element dateTimeHeader = ElementFactory.createHeading4(
                    arrangeDateTime.format(DateTimeFormatter.ofPattern("M-dd HH:mm")));
            getContent().getElement().insertChild(0, dateTimeHeader);
        }

        private void buildItemsContent(
                Map<SchedulingFactoryInstance, Map<SchedulingProduct, Long>> factoryAndArrangements
        ) {
            HorizontalLayout itemsContent = new HorizontalLayout();
            itemsContent.addClassNames(LumoUtility.FlexWrap.WRAP);
            factoryAndArrangements.entrySet()
                    .stream()
                    .map(
                            factoryArrangementsMapEntry -> {
                                Card card = new Card();
                                String factory = Optional.ofNullable(factoryArrangementsMapEntry.getKey()
                                                .getFactoryReadableIdentifier())
                                        .map(
                                                FactoryReadableIdentifier::getFactoryCategory)
                                        .orElse("N/A")
                                        ;
                                card.setTitle(factory);

                                Div itemAmountPairsDiv = new Div();
                                itemAmountPairsDiv.addClassNames(
                                        LumoUtility.Display.FLEX,
                                        LumoUtility.Overflow.AUTO,
                                        LumoUtility.Gap.SMALL,
                                        LumoUtility.Margin.Horizontal.XSMALL,
                                        LumoUtility.Height.AUTO
                                );

                                factoryArrangementsMapEntry.getValue().entrySet()
                                        .stream()
                                        .map((productAmountEntry) -> {
                                            Span span = new Span();
                                            String productName = Optional.ofNullable(productAmountEntry.getKey()
                                                            .getName())
                                                    .orElse("N/A");
                                            span.add(getProductImage(productName));
                                            span.add(productName);
                                            span.add(" x" + productAmountEntry.getValue());
                                            return span;
                                        })
                                        .forEach(card::add)
                                ;

                                return card;
                            }
                    )
                    .forEach(itemsContent::add)
            ;
            getContent().add(itemsContent);
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
