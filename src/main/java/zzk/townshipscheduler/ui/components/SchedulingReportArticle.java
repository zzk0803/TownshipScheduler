package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.ui.pojo.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
public class SchedulingReportArticle extends Composite<VerticalLayout> {

    private TownshipSchedulingProblemViewModel townshipSchedulingProblemViewModel;

    private Function<String, Image> fetchImgByIdProvider;

    private Button button;

    private VerticalLayout contentLayout;

    public SchedulingReportArticle(
            TownshipSchedulingProblemViewModel townshipSchedulingProblemViewModel,
            Function<String, Image> fetchImgByIdProvider
    ) {
        this(townshipSchedulingProblemViewModel);
        this.fetchImgByIdProvider = fetchImgByIdProvider;

        button = new Button(
                VaadinIcon.REFRESH.create(),
                click -> {
                    update(this.townshipSchedulingProblemViewModel);
                }
        );
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        contentLayout = new VerticalLayout();
    }

    public SchedulingReportArticle(TownshipSchedulingProblemViewModel townshipSchedulingProblemViewModel) {
        this.townshipSchedulingProblemViewModel = townshipSchedulingProblemViewModel;
    }

    public void update(TownshipSchedulingProblemViewModel townshipSchedulingProblem) {
        this.townshipSchedulingProblemViewModel = townshipSchedulingProblem;
        update();
    }

    private void update() {
        contentLayout.removeAll();

        if (townshipSchedulingProblemViewModel != null) {
            buildContentWithSolution(townshipSchedulingProblemViewModel);
        } else {
            buildEmptyContent();
        }
    }

    private void buildContentWithSolution(TownshipSchedulingProblemViewModel townshipSchedulingProblemViewModel) {
        buildWithArrangementsContent(townshipSchedulingProblemViewModel.schedulingProducingArrangementViewModels());
    }

    private void buildWithArrangementsContent(Collection<SchedulingProducingArrangementViewModel> schedulingProducingArrangementList) {
        var byDateTimeByFactoryByProductMapToCount = toSchedulingReportGroupsViewModel(schedulingProducingArrangementList);
        ByDateTimeByFactoryByProductMapToCountGrid grid = new ByDateTimeByFactoryByProductMapToCountGrid();
        grid.setItems(byDateTimeByFactoryByProductMapToCount.schedulingReportArrangeDateTimeGroupViewModels());
        grid.addComponentColumn(DateTimeFactoryArrangementsCard::new);
        addErrorSpanIfNotFeasible();
        contentLayout.addAndExpand(grid);
    }

    private SchedulingReportGroupsViewModel toSchedulingReportGroupsViewModel(Collection<SchedulingProducingArrangementViewModel> schedulingProducingArrangementList) {
        ArrayList<SchedulingReportArrangeDateTimeGroupViewModel> schedulingReportArrangeDateTimeGroupViewModels = schedulingProducingArrangementList.stream()
                .filter(schedulingProducingArrangement -> Objects.nonNull(schedulingProducingArrangement.arrangeDateTime()))
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(
                                        SchedulingProducingArrangementViewModel::arrangeDateTime,
                                        TreeMap::new,
                                        Collectors.collectingAndThen(
                                                Collectors.groupingBy(
                                                        SchedulingProducingArrangementViewModel::assignedFactoryInstance,
                                                        Collectors.collectingAndThen(
                                                                Collectors.groupingBy(
                                                                        SchedulingProducingArrangementViewModel::product,
                                                                        Collectors.counting()
                                                                ),
                                                                schedulingProductViewModelLongMap -> {
                                                                    Collection<SchedulingProductAmountPair> schedulingProductAmountPairs
                                                                            = schedulingProductViewModelLongMap.entrySet()
                                                                            .stream()
                                                                            .map(
                                                                                    schedulingProductViewModelLongEntry -> {
                                                                                        return new SchedulingProductAmountPair(
                                                                                                schedulingProductViewModelLongEntry.getKey(),
                                                                                                Math.toIntExact(schedulingProductViewModelLongEntry.getValue())
                                                                                        );
                                                                                    })
                                                                            .collect(Collectors.toCollection(ArrayList::new));
                                                                    return new ProductAmountBillViewModel(schedulingProductAmountPairs);
                                                                }
                                                        )
                                                ),
                                                schedulingFactoryInstanceViewModelProductAmountBillViewModelMap -> {
                                                    return schedulingFactoryInstanceViewModelProductAmountBillViewModelMap.entrySet()
                                                            .stream()
                                                            .map(schedulingFactoryInstanceViewModelProductAmountBillViewModelEntry -> {
                                                                return new SchedulingReportFactoryGroupViewModel(
                                                                        schedulingFactoryInstanceViewModelProductAmountBillViewModelEntry.getKey(),
                                                                        schedulingFactoryInstanceViewModelProductAmountBillViewModelEntry.getValue()
                                                                );
                                                            })
                                                            .collect(Collectors.toCollection(ArrayList::new));
                                                }
                                        )
                                ),
                                localDateTimeCollectionTreeMap -> {
                                    return localDateTimeCollectionTreeMap.entrySet().stream().map(localDateTimeCollectionEntry -> {
                                        LocalDateTime arrangeDateTime = localDateTimeCollectionEntry.getKey();
                                        Collection<SchedulingReportFactoryGroupViewModel> localDateTimeCollectionEntryValue = localDateTimeCollectionEntry.getValue();
                                        return new SchedulingReportArrangeDateTimeGroupViewModel(
                                                arrangeDateTime,
                                                localDateTimeCollectionEntryValue
                                        );
                                    });
                                }
                        )
                ).collect(Collectors.toCollection(ArrayList::new));
        return new SchedulingReportGroupsViewModel(schedulingReportArrangeDateTimeGroupViewModels);
    }

    private void addErrorSpanIfNotFeasible() {
        if (getTownshipSchedulingProblemViewModel().feasible()) {
            Span span = new Span("Eureka");
            span.getElement().getThemeList().add("badge success");
            contentLayout.add(span);
        } else {
            Span span = new Span("Not Feasible");
            span.getElement().getThemeList().add("badge  error");
            contentLayout.add(span);
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

    public void updateScheduling(TownshipSchedulingProblemViewModel townshipSchedulingProblem) {
        setTownshipSchedulingProblemViewModel(townshipSchedulingProblem);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        update();
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout verticalLayout = super.initContent();
        verticalLayout.add(button);
        verticalLayout.addAndExpand(contentLayout);
        return verticalLayout;
    }

    private Image getProductImage(String productName) {
        return this.fetchImgByIdProvider.apply(productName);
    }


    static class ByDateTimeByFactoryByProductMapToCountGrid
            extends Grid<SchedulingReportArrangeDateTimeGroupViewModel> {

    }

    class DateTimeFactoryArrangementsCard extends Composite<HorizontalLayout> {

        public DateTimeFactoryArrangementsCard(
                SchedulingReportArrangeDateTimeGroupViewModel schedulingReportArrangeDateTimeGroupViewModel
        ) {
            LocalDateTime arrangeDateTime = schedulingReportArrangeDateTimeGroupViewModel.arrangeDateTime();
            var factoryAndArrangements = schedulingReportArrangeDateTimeGroupViewModel.schedulingReportFactoryGroupViewModels();

            buildItemsContent(factoryAndArrangements);
            buildDateTimeContent(arrangeDateTime);
        }

        private void buildItemsContent(
                Collection<SchedulingReportFactoryGroupViewModel> factoryAndArrangements
        ) {
            HorizontalLayout itemsContent = new HorizontalLayout();
            itemsContent.addClassNames(LumoUtility.FlexWrap.WRAP);
            factoryAndArrangements.stream()
                    .map(
                            schedulingReportFactoryGroupViewModel -> {
                                Card card = new Card();
                                String factoryReadableIdentifier = schedulingReportFactoryGroupViewModel.schedulingFactoryInstanceViewModel()
                                        .factoryReadableIdentifier();
                                ProductAmountBillViewModel productAmountBillViewModel = schedulingReportFactoryGroupViewModel.productAmountBillViewModel();

                                String factory = Optional.ofNullable(factoryReadableIdentifier)
                                        .orElse("N/A");
                                card.setTitle(factory);

                                Div itemAmountPairsDiv = new Div();
                                itemAmountPairsDiv.addClassNames(
                                        LumoUtility.Display.FLEX,
                                        LumoUtility.Overflow.AUTO,
                                        LumoUtility.Gap.SMALL,
                                        LumoUtility.Margin.Horizontal.XSMALL,
                                        LumoUtility.Height.AUTO
                                );

                                productAmountBillViewModel.productAmountPairs()
                                        .stream()
                                        .map((schedulingProductAmountPair) -> {
                                            Span span = new Span();
                                            String productName = Optional.ofNullable(
                                                            schedulingProductAmountPair.product().name()
                                                    )
                                                    .orElse("N/A");
                                            span.add(getProductImage(productName));
                                            span.add(productName);
                                            span.add(" x" + schedulingProductAmountPair.amount());
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

        private void buildDateTimeContent(LocalDateTime arrangeDateTime) {
            Element dateTimeHeader = ElementFactory.createHeading4(
                    arrangeDateTime.format(DateTimeFormatter.ofPattern("M-dd HH:mm")));
            getContent().getElement().insertChild(
                    0,
                    dateTimeHeader
            );
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
