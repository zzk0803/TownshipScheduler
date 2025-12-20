package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.SerializablePredicate;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;
import zzk.townshipscheduler.ui.utility.UiEventBus;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ProductsAmountPanel extends Composite<VerticalLayout> {

    private final TextField filterTextField;

    private final Grid<FieldFactoryInfoEntity> factoryProductsGrid;

    private final Supplier<Collection<FieldFactoryInfoEntity>> factoryProductsSupplier;

    private Collection<FieldFactoryInfoEntity> fieldFactoryInfoEntities;

    private List<ProductEntity> productEntityList;

    public ProductsAmountPanel(Supplier<Collection<FieldFactoryInfoEntity>> factoryProductsSupplier) {
        this.factoryProductsSupplier = factoryProductsSupplier;
        factoryProductsGrid = createGrid();
        getContent().addAndExpand(factoryProductsGrid);

        filterTextField = new TextField();
        filterTextField.setWidthFull();
        filterTextField.setPlaceholder("Filter Products...");
        filterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        filterTextField.addValueChangeListener(valueChangeEvent -> {
            String criteria = valueChangeEvent.getValue().toLowerCase();
            GridListDataView<FieldFactoryInfoEntity> dataView = factoryProductsGrid.getListDataView();
            dataView.removeFilters();
            dataView.addFilter(createTextFieldGridFilter(criteria));
            dataView.refreshAll();
        });
        filterTextField.setPrefixComponent(VaadinIcon.SEARCH.create());
        Button textFieldSuffixButton = new Button(
                VaadinIcon.CLOSE_SMALL.create(),
                clicked -> {
                    filterTextField.clear();
                    GridListDataView<FieldFactoryInfoEntity> dataView = factoryProductsGrid.getListDataView();
                    dataView.removeFilters();
                    dataView.refreshAll();
                }
        );
        textFieldSuffixButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        filterTextField.setSuffixComponent(textFieldSuffixButton);
        UiEventBus.subscribe(
                filterTextField,
                ProductCardProductSpanClickedEvent.class,
                componentEvent -> {
                    filterTextField.setValue(componentEvent.getProductName());
                }
        );
        HorizontalLayout filterWrapper = new HorizontalLayout();
        filterWrapper.setWidthFull();
        filterWrapper.add(filterTextField);
        filterWrapper.setFlexGrow(1.0d, filterTextField);
        filterWrapper.setFlexShrink(1.0d, filterTextField);
        getContent().addComponentAsFirst(filterWrapper);
    }

    private Grid<FieldFactoryInfoEntity> createGrid() {
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

        return grid;
    }

    private SerializablePredicate<FieldFactoryInfoEntity> createTextFieldGridFilter(String criteria) {
        return fieldFactoryInfoEntity -> {
            String factoryName = fieldFactoryInfoEntity.getCategory();
            return factoryName.contains(criteria)
                   || fieldFactoryInfoEntity.getPortfolioGoods().stream()
                           .anyMatch(productEntity -> {
                               return productEntity.getName()
                                              .toLowerCase()
                                              .contains(criteria)
                                      || productEntity.getBomString()
                                              .toLowerCase()
                                              .contains(criteria);
                           })
                   || fieldFactoryInfoEntity.getPortfolioGoods().stream()
                           .map(ProductEntity::getBomString)
                           .anyMatch(productBomString -> {
                               return productBomString.toLowerCase().contains(criteria);
                           });
        };
    }

    private Button createFilterButton() {
        Button button = new Button();
        button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        button.setIcon(VaadinIcon.FILTER.create());

        Popover popover = new Popover();
        popover.setTarget(button);
        popover.addThemeVariants(PopoverVariant.ARROW);

        RadioButtonGroup<RadioButtonGroupValues> productFilterRbg = new RadioButtonGroup<>();
        productFilterRbg.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        productFilterRbg.setItems(RadioButtonGroupValues.values());
        productFilterRbg.setValue(RadioButtonGroupValues.EVERYTHING);
        productFilterRbg.addValueChangeListener(changed -> {
            this.factoryProductsGrid.getListDataView().removeFilters();
            this.factoryProductsGrid.getListDataView()
                    .addFilter(createTextFieldGridFilter(this.filterTextField.getValue()));
            this.factoryProductsGrid.getListDataView().addFilter(fieldFactoryInfoEntity -> {
                boolean result = false;
                RadioButtonGroupValues value = changed.getValue();
                switch (value) {
                    case ATOMIC -> {
                        result = isAtomicProductFilter(fieldFactoryInfoEntity);
                    }
                    case INTERMEDIATE -> {
                        result = !isAtomicProductFilter(fieldFactoryInfoEntity)
                                 && !isFinalProductFilter(fieldFactoryInfoEntity);
                    }
                    case FINAL -> {
                        result = isFinalProductFilter(fieldFactoryInfoEntity);
                    }
                    case EVERYTHING -> {
                        return result = true;
                    }
                }
                return result;
            });

            this.factoryProductsGrid.getListDataView().refreshAll();
        });
        popover.add(productFilterRbg);

        return button;
    }

    private  boolean isAtomicProductFilter(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
        boolean result;
        result = fieldFactoryInfoEntity.getPortfolioGoods().stream()
                .anyMatch(productEntity -> productEntity.getBomString().isBlank());
        return result;
    }

    private boolean isFinalProductFilter(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
        boolean result;
        result = fieldFactoryInfoEntity.getPortfolioGoods().stream()
                .anyMatch(subjectProduct -> subjectProductComposite(subjectProduct).isEmpty());
        return result;
    }

    private List<ProductEntity> subjectProductComposite(ProductEntity subjectProduct) {
        return this.productEntityList.stream()
                .filter(product -> {
                    return product.getManufactureInfoEntities().stream()
                            .flatMap(productManufactureInfoEntity -> productManufactureInfoEntity.getProductMaterialsRelations()
                                    .stream()
                            )
                            .anyMatch(productMaterialsRelation -> Objects.equals(productMaterialsRelation.getMaterial(),subjectProduct));
                })
                .toList();
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout verticalLayout = super.initContent();
        verticalLayout.setMargin(false);
        return verticalLayout;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.filterTextField.clear();

        this.fieldFactoryInfoEntities = factoryProductsSupplier.get();
        this.productEntityList = this.fieldFactoryInfoEntities.stream()
                .flatMap(fieldFactoryInfoEntity -> fieldFactoryInfoEntity.getPortfolioGoods().stream())
                .toList();
        this.factoryProductsGrid.setItems(fieldFactoryInfoEntities);
    }

    private enum RadioButtonGroupValues {
        EVERYTHING("Everything"),
        ATOMIC("Atomic"),
        INTERMEDIATE("Intermediate"),
        FINAL("Final");

        private final String string;

        RadioButtonGroupValues(String string) {
            this.string = string;
        }
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
            Element nameSpan = ElementFactory.createSpan(productEntity.getName());
            nameSpan.getStyle().setCursor("pointer");
            nameSpan.getStyle().setBorderBottom("1px solid black");
            nameSpan.addEventListener(
                    "click", domEvent -> {
                        UiEventBus.publish(
                                new ProductCardProductSpanClickedEvent(this, false, productEntity.getName())
                        );
                    }
            );
            getContent().add(createImage(productEntity));
            getContent().getElement()
                    .appendChild(nameSpan);
            getContent().getElement()
                    .appendChild(ElementFactory.createSpan("Level:" + productEntity.getLevel().toString()));
            getContent().add(createAmountField(productEntity));
        }

        private  Image createImage(ProductEntity productEntity) {
            WikiCrawledEntity crawledAsImage = productEntity.getCrawledAsImage();
            return ProductImages.productImage(
                    productEntity.getName(),
                    crawledAsImage
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

    public static class ProductCardProductSpanClickedEvent extends ComponentEvent<ProductCard> {

        private final String productName;

        public ProductCardProductSpanClickedEvent(ProductCard source, boolean fromClient, String productName) {
            super(source, fromClient);
            this.productName = productName;
        }

        public String getProductName() {
            return productName;
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
