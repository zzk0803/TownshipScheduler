package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
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

        TextField filterTextField = new TextField();
        filterTextField.setWidthFull();
        filterTextField.setPlaceholder("Filter Products...");
        filterTextField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        filterTextField.addValueChangeListener(valueChangeEvent -> {
            String criteria = valueChangeEvent.getValue().toLowerCase();
            GridListDataView<FieldFactoryInfoEntity> dataView = factoryProductsGrid.getListDataView();
            if (criteria.isBlank()) {
                dataView.removeFilters();
            } else {
                dataView.addFilter(fieldFactoryInfoEntity -> {
                    String factoryName = fieldFactoryInfoEntity.getCategory();
                    return factoryName.contains(criteria) || fieldFactoryInfoEntity.getPortfolioGoods().stream()
                            .anyMatch(productEntity -> {
                                return productEntity.getName().contains(criteria) || productEntity.getBomString()
                                        .contains(criteria);
                            });
                });
            }
            dataView.refreshAll();
        });
        filterTextField.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterTextField.setSuffixComponent(new Button(
                VaadinIcon.CLOSE_SMALL.create(), clicked -> {
            filterTextField.clear();
            GridListDataView<FieldFactoryInfoEntity> dataView = factoryProductsGrid.getListDataView();
            dataView.removeFilters();
            dataView.refreshAll();
        }
        ));
        getContent().addComponentAsFirst(filterTextField);
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
        grid.setItems(factoryProductsSupplier.get());
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
            productsGridLayout.setWrap(true);
            fieldFactoryInfoEntity.getPortfolioGoods().stream()
                    .sorted(Comparator.comparingInt(ProductEntity::getLevel))
                    .map(ProductCard::new)
                    .forEachOrdered(productsGridLayout::add);

            getContent().add(factoryHeaderLayout, productsGridLayout);
        }

        @Override
        protected VerticalLayout initContent() {
            VerticalLayout verticalLayout = super.initContent();
            verticalLayout.setWidth(95.0f, Unit.VW);
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


        @Override
        protected VerticalLayout initContent() {
            VerticalLayout verticalLayout = super.initContent();
            verticalLayout.setWidth(200, Unit.PIXELS);
            return verticalLayout;
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
