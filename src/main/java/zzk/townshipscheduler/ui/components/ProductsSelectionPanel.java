package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.StreamResource;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.ui.utility.UiEventBus;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Supplier;

public class ProductsSelectionPanel extends Composite<VerticalLayout> {

    public ProductsSelectionPanel(Supplier<Collection<FieldFactoryInfoEntity>> factoryProductsSupplier) {
        Grid<FieldFactoryInfoEntity> factoryProductsGrid = createGrid(factoryProductsSupplier);
        getContent().addAndExpand(factoryProductsGrid);
    }

    private Grid<FieldFactoryInfoEntity> createGrid(Supplier<Collection<FieldFactoryInfoEntity>> factoryProductsSupplier) {
        final Grid<FieldFactoryInfoEntity> grid;
        grid = new Grid<>(FieldFactoryInfoEntity.class, false);
        grid.setId("goods-categories-selection-grid");
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setHeightFull();
        grid.setWidthFull();
        grid.addColumn(new ComponentRenderer<>(FactoryProductsCard::new))
                .setComparator(Comparator.comparingInt(FieldFactoryInfoEntity::getLevel))
                .setFlexGrow(1);
        grid.setDataProvider(DataProvider.ofCollection(factoryProductsSupplier.get()));
        return grid;
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout verticalLayout = super.initContent();
        verticalLayout.setMargin(false);
        return verticalLayout;
    }

    private static class FactoryProductsCard extends Composite<VerticalLayout> {

        public FactoryProductsCard(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
            HorizontalLayout factoryHeaderLayout = new HorizontalLayout();
            Element category = ElementFactory.createHeading2(fieldFactoryInfoEntity.getCategory());
            Element level = ElementFactory.createSpan(String.valueOf(fieldFactoryInfoEntity.getLevel()));
            factoryHeaderLayout.getElement().appendChild(category, level);

            HorizontalLayout productsGridLayout = new HorizontalLayout();
            productsGridLayout.setWidthFull();
            fieldFactoryInfoEntity.getPortfolioGoods().stream().map(ProductCard::new).forEach(productsGridLayout::add);

            Scroller productsGridLayoutScroller = new Scroller();
            productsGridLayoutScroller.setWidthFull();
            productsGridLayoutScroller.setScrollDirection(Scroller.ScrollDirection.HORIZONTAL);
            productsGridLayoutScroller.setContent(productsGridLayout);

            getContent().add(factoryHeaderLayout, productsGridLayoutScroller);
        }

        @Override
        protected VerticalLayout initContent() {
            VerticalLayout verticalLayout = super.initContent();
            verticalLayout.setSpacing(2.0f, Unit.PIXELS);
            verticalLayout.setMinWidth(95.0f, Unit.VW);
            return verticalLayout;
        }

    }

    private static class ProductCard extends Composite<VerticalLayout> {

        public ProductCard(ProductEntity productEntity) {
            getContent().add(createImage(productEntity));
            getContent().getElement().appendChild(ElementFactory.createSpan("Name:" + productEntity.getName()));
            getContent().getElement()
                    .appendChild(ElementFactory.createSpan("Level:" + productEntity.getLevel().toString()));
            getContent().add(createAmountField(productEntity));
        }

        private static Image createImage(ProductEntity productEntity) {
            return new Image(
                    new StreamResource(
                            productEntity.getName(),
                            () -> new ByteArrayInputStream(productEntity.getCrawledAsImage().getImageBytes())
                    ),
                    productEntity.getName()
            );
        }

        private IntegerField createAmountField(ProductEntity productEntity) {
            IntegerField amountField = new IntegerField();
            amountField.setPlaceholder("Amount");
            amountField.setValue(0);
            amountField.setMin(0);
            amountField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
            amountField.addValueChangeListener(valueChangeEvent -> {
                Integer value = valueChangeEvent.getValue();
                UiEventBus.publish(
                        new ProductCardSelectionAmountEvent(
                                this,
                                false,
                                productEntity,
                                value
                        )
                );
            });
            return amountField;
        }


    }

    public static class ProductCardSelectionAmountEvent extends ComponentEvent<ProductCard> {

        private final ProductEntity product;

        private final int amount;

        public ProductCardSelectionAmountEvent(
                ProductCard source,
                boolean fromClient,
                ProductEntity productEntity,
                int amount
        ) {
            super(source, fromClient);
            this.product = productEntity;
            this.amount = amount;
        }

        public ProductEntity getProduct() {
            return product;
        }

        public int getAmount() {
            return amount;
        }

    }

}
