package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cache.annotation.Cacheable;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingOrder;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingProducingArrangement;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;
import zzk.townshipscheduler.ui.pojo.SchedulingProducingArrangementVO;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
public class SchedulingReportArticle extends Composite<VerticalLayout> {

    private TownshipSchedulingProblem townshipSchedulingProblem;

    private Function<String, byte[]> fetchImgByIdProvider;

    public SchedulingReportArticle(TownshipSchedulingProblem townshipSchedulingProblem) {
        this.townshipSchedulingProblem = townshipSchedulingProblem;
    }

    public SchedulingReportArticle(
            TownshipSchedulingProblem townshipSchedulingProblem,
            Function<String, byte[]> fetchImgByIdProvider
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
        addErrorSpanIfNotFeasible();
        getContent().addAndExpand(grid);
    }

    private void addErrorSpanIfNotFeasible() {
        if (!getTownshipSchedulingProblem().getScore().isFeasible()) {
            Span span = new Span("Not Feasible");
            span.getElement().getThemeList().add("badge contrast error");
            getContent().add(span);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        update();
    }

    private Image createProductImage(String productName) {
        byte[] productImage = fetchImgByIdProvider.apply(productName);
        Image image = new Image(
                new StreamResource(productName, () -> new ByteArrayInputStream(productImage)),
                productName
        );
        image.setWidth("30px");
        image.setHeight("30px");

        return image;
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

                        productCountMap.entrySet()
                                .stream()
                                .map((productAmountEntry) -> {
                                    Span span = new Span();
                                    String productName = productAmountEntry.getKey();
                                    span.add(createProductImage(productName));
                                    span.add(productName);
                                    span.add(" x" + productAmountEntry.getValue());
                                    return span;
                                })
                                .forEach(div::add);

                        Article article = new Article();
                        article.addClassNames(LumoUtility.Width.AUTO, LumoUtility.Height.AUTO);
                        article.getElement().appendChild(factoryHeader);
                        article.add(div);
                        return article;
                    })
                    .forEach(article -> {
                        getContent().add(article);
                    });

            Element dateTimeHeader = ElementFactory.createHeading4(arrangeDateTime.format(DateTimeFormatter.ofPattern(
                    "M-dd HH:mm")));
            getContent().getElement().insertChild(0, dateTimeHeader);
        }

        @Override
        protected HorizontalLayout initContent() {
            HorizontalLayout horizontalLayout = super.initContent();
            horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            horizontalLayout.setAlignItems(FlexComponent.Alignment.START);
            horizontalLayout.setWrap(true);
            return horizontalLayout;
        }

    }

}
