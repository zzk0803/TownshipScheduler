package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.ui.pojo.scheduling.*;
import zzk.townshipscheduler.ui.views.scheduling.SchedulingView;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

@Getter
@Setter
public class SchedulingReportArticle
        extends Composite<VerticalLayout> {

    private final Function<String, Image> fetchImgByIdProvider;

    private final SchedulingReportArrangeDateTimeGroupsGrid reportGroupsGrid;

    private final Signal<SchedulingReportGroupsViewModel> schedulingReportGroupsViewModelSignal;

    public SchedulingReportArticle(
            SchedulingView schedulingView,
            Function<String, Image> fetchImgByIdProvider
    ) {
        this.fetchImgByIdProvider = fetchImgByIdProvider;

        this.reportGroupsGrid = new SchedulingReportArrangeDateTimeGroupsGrid();
        this.reportGroupsGrid.addComponentColumn(DateTimeFactoryArrangementsCard::new);

        this.schedulingReportGroupsViewModelSignal = schedulingView.getReactiveTownshipSchedulingProblemViewModelValueSignal().map(
                townshipSchedulingProblemViewModel -> {
                    if (townshipSchedulingProblemViewModel == null
                        || TownshipSchedulingProblemViewModel.EMPTY_NULL_VALUE.equals(townshipSchedulingProblemViewModel)
                    ) {
                        return SchedulingReportGroupsViewModel.EMPTY_NULL_VALUE;
                    }
                    return townshipSchedulingProblemViewModel.toSchedulingReportGroupsViewModel();
                });

        Span span = new Span();
        span.getElement()
                .getThemeList()
                .add("badge");
        span.bindText(
                schedulingView.getReactiveTownshipSchedulingProblemViewModelValueSignal().get()
                        .feasible().map(value -> value
                                ? "Eureka"
                                : "Not Feasible"
                        )
        );

        this.getContent().
                add(span);
        this.getContent().
                addAndExpand(reportGroupsGrid);

        Signal.effect(
                reportGroupsGrid,
                () -> {
                    reportGroupsGrid.setItems(this.schedulingReportGroupsViewModelSignal.get().schedulingReportArrangeDateTimeGroupViewModels());
                }
        );
    }

    private Image getProductImage(String productName) {
        return this.fetchImgByIdProvider.apply(productName);
    }


    static class SchedulingReportArrangeDateTimeGroupsGrid
            extends Grid<SchedulingReportArrangeDateTimeGroupViewModel> {

        @Serial
        private static final long serialVersionUID = -1702605345408428854L;

    }

    class DateTimeFactoryArrangementsCard
            extends Composite<HorizontalLayout> {

        @Serial
        private static final long serialVersionUID = 6775155059195776013L;

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
                                                            schedulingProductAmountPair.product()
                                                                    .name()
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
            getContent().getElement()
                    .insertChild(
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
