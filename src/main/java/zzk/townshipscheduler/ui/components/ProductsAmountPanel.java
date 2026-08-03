package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
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
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.ui.utility.VaadinUiEventBus;

import java.io.Serial;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProductsAmountPanel
        extends Composite<VerticalLayout> {

    private final TextField filterTextField;

    private final Grid<FieldFactoryInfoEntity> factoryProductsGrid;

    private final Supplier<Collection<FieldFactoryInfoEntity>> factoryProductsSupplier;

    private final Consumer<Map<ProductEntity, Integer>> markedProductsConsumer;

//    private final Map<ProductEntity, Integer> markedProducts = new LinkedHashMap<>();

    private final ValueSignal<Map<ProductEntity, Integer>> markedProductsSignals = new ValueSignal<>(new LinkedHashMap<>());

    private List<ProductEntity> productEntityList;

    private RadioButtonGroup<RadioButtonGroupValues> productFilterRbg;

    public ProductsAmountPanel(
            Supplier<Collection<FieldFactoryInfoEntity>> factoryProductsSupplier,
            Consumer<Map<ProductEntity, Integer>> markedProductsConsumer,
            Map<ProductEntity, Integer> itemAmountMap
    ) {
        this(factoryProductsSupplier, markedProductsConsumer);
//        this.markedProducts.putAll(itemAmountMap);
        this.markedProductsSignals.modify(mapInSignal -> {
            if (mapInSignal == null) {
                this.markedProductsSignals.set(new LinkedHashMap<>(itemAmountMap));
                return;
            }
            mapInSignal.putAll(itemAmountMap);
        });
    }

    public ProductsAmountPanel(
            Supplier<Collection<FieldFactoryInfoEntity>> factoryProductsSupplier,
            Consumer<Map<ProductEntity, Integer>> markedProductsConsumer
    ) {
        this.factoryProductsSupplier = factoryProductsSupplier;
        this.markedProductsConsumer = markedProductsConsumer;
        factoryProductsGrid = createGrid();
        getContent().addAndExpand(factoryProductsGrid);

        filterTextField = new TextField();
        filterTextField.setWidthFull();
        filterTextField.setPlaceholder("Filter Products...");
        filterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        filterTextField.addValueChangeListener(valueChangeEvent -> {
            String criteria = valueChangeEvent.getValue()
                    .toLowerCase();
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
                    productFilterRbg.setValue(RadioButtonGroupValues.EVERYTHING);
                    GridListDataView<FieldFactoryInfoEntity> dataView = factoryProductsGrid.getListDataView();
                    dataView.removeFilters();
                    dataView.refreshAll();
                }
        );
        textFieldSuffixButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        filterTextField.setSuffixComponent(new HorizontalLayout(createFilterButton(), textFieldSuffixButton));
        VaadinUiEventBus.subscribe(
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
        final Grid<FieldFactoryInfoEntity> grid = new Grid<>(FieldFactoryInfoEntity.class, false);
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
            Set<ProductEntity> productEntities = fieldFactoryInfoEntity.getProductEntities();
            return factoryName.contains(criteria)
                   || productEntities.stream()
                           .anyMatch(productEntity -> {
                               return productEntity.getName()
                                              .toLowerCase()
                                              .contains(criteria)
                                      || productEntity.getBomString()
                                              .toLowerCase()
                                              .contains(criteria);
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
        popover.setOpenOnHover(true);

        productFilterRbg = new RadioButtonGroup<>();
        productFilterRbg.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        productFilterRbg.setItems(RadioButtonGroupValues.values());
        productFilterRbg.setValue(RadioButtonGroupValues.EVERYTHING);
        productFilterRbg.addValueChangeListener(changed -> {
            GridListDataView<FieldFactoryInfoEntity> listDataView = this.factoryProductsGrid.getListDataView();
            listDataView.removeFilters();
            listDataView
                    .addFilter(
                            createTextFieldGridFilter(this.filterTextField.getValue())
                    );
            listDataView.addFilter(
                    fieldFactoryInfoEntity -> {
                        RadioButtonGroupValues value = changed.getValue();
                        switch (value) {
                            case ATOMIC -> {
                                return isAtomicProductFilter(fieldFactoryInfoEntity);
                            }
                            case INTERMEDIATE -> {
                                return !isAtomicProductFilter(fieldFactoryInfoEntity)
                                       && !isFinalProductFilter(fieldFactoryInfoEntity);
                            }
                            case FINAL -> {
                                return isFinalProductFilter(fieldFactoryInfoEntity);
                            }
                            case EVERYTHING -> {
                                return true;
                            }
                        }
                        return false;
                    });

            listDataView.refreshAll();
        });
        popover.add(productFilterRbg);

        return button;
    }

    private boolean isAtomicProductFilter(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
        boolean result;
        result = fieldFactoryInfoEntity.getProductEntities()
                .stream()
                .anyMatch(productEntity -> productEntity.getBomString()
                        .isBlank());
        return result;
    }

    private boolean isFinalProductFilter(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
        boolean result;
        result = fieldFactoryInfoEntity.getProductEntities()
                .stream()
                .anyMatch(productEntity -> subjectProductComposite(productEntity).isEmpty());
        return result;
    }

    private List<ProductEntity> subjectProductComposite(ProductEntity productEntity) {
        return this.productEntityList.stream()
                .filter(product -> {
                    return product.getManufactureInfoEntities()
                            .stream()
                            .flatMap(productManufactureInfoEntity -> productManufactureInfoEntity.getProductMaterialsRelations()
                                    .stream()
                            )
                            .anyMatch(productMaterialsRelation -> Objects.equals(productMaterialsRelation.getMaterial(), productEntity));
                })
                .toList();
    }

    public synchronized void consume() {
        this.markedProductsConsumer.accept(this.markedProductsSignals.peek());
        this.markedProductsSignals.modify(map -> {
            if (map == null) {
                this.markedProductsSignals.set(new LinkedHashMap<>());
                return;
            }
            map.clear();
        });
//        this.markedProducts.clear();
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout verticalLayout = super.initContent();
        verticalLayout.setMargin(false);
        verticalLayout.setMaxWidth(95, Unit.PERCENTAGE);
        verticalLayout.setMinWidth(70, Unit.PERCENTAGE);
        return verticalLayout;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.filterTextField.clear();

        Collection<FieldFactoryInfoEntity> fieldFactoryInfoEntities = factoryProductsSupplier.get();
        this.productEntityList = fieldFactoryInfoEntities.stream()
                .flatMap(fieldFactoryInfoEntity -> fieldFactoryInfoEntity.getProductEntities()
                        .stream()
                )
                .toList();
        this.factoryProductsGrid.setItems(fieldFactoryInfoEntities);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.markedProductsSignals.modify(map -> {
            if (map == null) {
                this.markedProductsSignals.set(new LinkedHashMap<>());
                return;
            }
            map.clear();
        });
    }

    private enum RadioButtonGroupValues {
        EVERYTHING,
        ATOMIC,
        INTERMEDIATE,
        FINAL
    }

    public static class ProductCardProductSpanClickedEvent
            extends ComponentEvent<ProductCard> {

        @Serial
        private static final long serialVersionUID = -6166860285068559140L;

        private final String productName;

        public ProductCardProductSpanClickedEvent(
                ProductCard source,
                boolean fromClient,
                String productName
        ) {
            super(source, fromClient);
            this.productName = productName;
        }

        public String getProductName() {
            return productName;
        }

    }

    public static class ProductCardSelectionAmountEvent
            extends ComponentEvent<ProductCard> {

        @Serial
        private static final long serialVersionUID = -8077793289788708966L;

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

    private class FactoryProductsCard
            extends Composite<VerticalLayout> {

        @Serial
        private static final long serialVersionUID = 4572783553859248795L;

        public FactoryProductsCard(FieldFactoryInfoEntity fieldFactoryInfoEntity) {
            HorizontalLayout factoryHeaderLayout = new HorizontalLayout();
            Element category = ElementFactory.createHeading2(fieldFactoryInfoEntity.getCategory());
            category.getStyle()
                    .bind(
                            "color",
                            () -> {
                                Map<ProductEntity, Integer> productEntityIntegerMap = ProductsAmountPanel.this.markedProductsSignals.get();
                                if (fieldFactoryInfoEntity.getProductEntities()
                                        .stream()
                                        .anyMatch(productEntity -> productEntityIntegerMap.containsKey(productEntity) && productEntityIntegerMap.get(productEntity) > 0)) {
                                    return "var(--lumo-primary-color)";
                                } else {
                                    return "var(--lumo-header-text-color)";
                                }
                            }
                    );
            Element level = ElementFactory.createSpan(String.valueOf(fieldFactoryInfoEntity.getLevel()));
            factoryHeaderLayout.getElement()
                    .appendChild(category, level);

            AvatarGroup avatarGroup = new AvatarGroup(
                    fieldFactoryInfoEntity.getProductEntities()
                            .stream()
                            .map(ProductImages::productImageDownloadHandler)
                            .map(downloadHandler -> {
                                AvatarGroup.AvatarGroupItem avatarGroupItem = new AvatarGroup.AvatarGroupItem();
                                avatarGroupItem.setImageHandler(downloadHandler);
                                return avatarGroupItem;
                            })
                            .toList()
            );
            factoryHeaderLayout.getElement()
                    .appendChild(avatarGroup.getElement());

            HorizontalLayout productsGridLayout = new HorizontalLayout();
            productsGridLayout.setWrap(true);
            fieldFactoryInfoEntity.getProductEntities()
                    .stream()
                    .sorted(Comparator.comparingInt(ProductEntity::getLevel))
                    .map(ProductCard::new)
                    .forEachOrdered(productsGridLayout::add);

            Details factoryHeaderDetails = new Details(factoryHeaderLayout, productsGridLayout);
            factoryHeaderDetails.setWidthFull();

            Signal.effect(
                    factoryHeaderDetails,
                    () -> {
                        Map<ProductEntity, Integer> productEntityIntegerMap = ProductsAmountPanel.this.markedProductsSignals.get();
                        if (fieldFactoryInfoEntity.getProductEntities()
                                .stream()
                                .anyMatch(productEntityIntegerMap::containsKey)) {
                            factoryHeaderDetails.setOpened(true);
                        }
                    }
            );

            getContent().add(factoryHeaderDetails);

        }

        @Override
        protected VerticalLayout initContent() {
            VerticalLayout verticalLayout = super.initContent();
            verticalLayout.setMaxWidth(100, Unit.PERCENTAGE);
            verticalLayout.setMinWidth(80, Unit.PERCENTAGE);
            verticalLayout.setWrap(true);
            return verticalLayout;
        }

    }

    private class ProductCard
            extends Composite<VerticalLayout> {

        public ProductCard(ProductEntity productEntity) {
            Element nameSpan = ElementFactory.createSpan(productEntity.getName());
            nameSpan.getStyle()
                    .setCursor("pointer");
            nameSpan.getStyle()
                    .setBorderBottom("1px solid black");
            nameSpan.addEventListener(
                    "click",
                    domEvent -> {
                        VaadinUiEventBus.publish(
                                new ProductCardProductSpanClickedEvent(this, false, productEntity.getName())
                        );
                    }
            );
            getContent().add(createProductImage(productEntity));
            getContent().getElement()
                    .appendChild(nameSpan);
            getContent().getElement()
                    .appendChild(ElementFactory.createSpan("Level:" + productEntity.getLevel()
                            .toString()));
            getContent().add(createAmountField(productEntity));
        }

        private Image createProductImage(ProductEntity productEntity) {
            return ProductImages.productImage(
                    productEntity,
                    product -> product.getCrawledAsImage()
                            .getImageBytes()
            );
        }

        private IntegerField createAmountField(ProductEntity productEntity) {
            IntegerField amountField = new IntegerField();
            amountField.setPlaceholder("Amount");
//            amountField.setValue(markedProducts.getOrDefault(productEntity, 0));
            amountField.bindValue(
                    () -> ProductsAmountPanel.this.markedProductsSignals.get()
                            .getOrDefault(productEntity, 0),
                    integer -> {
                        if (integer <= 0) {
                            amountField.setValue(0);
                            ProductsAmountPanel.this.markedProductsSignals.modify(map -> {
                                if (map == null) {
                                    ProductsAmountPanel.this.markedProductsSignals.set(new LinkedHashMap<>());
                                    return;
                                }
                                map.remove(productEntity);
                            });
                            return;
                        }
                        ProductsAmountPanel.this.markedProductsSignals.modify(map -> {
                            if (map == null) {
                                ProductsAmountPanel.this.markedProductsSignals.set(new LinkedHashMap<>());
                                return;
                            }
                            map.put(productEntity, integer);
                        });
                    }
            );
            amountField.setMin(0);
            amountField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
//            amountField.addValueChangeListener(valueChangeEvent -> {
//                Integer value = valueChangeEvent.getValue();
//                if (value < 0) {
//                    amountField.setValue(0);
//                    ProductsAmountPanel.this.markedProducts.remove(productEntity);
//                    return;
//                }
//                ProductsAmountPanel.this.markedProducts.put(productEntity, value);
//            });
            amountField.setPrefixComponent(
                    new Button(VaadinIcon.MINUS.create()) {{
                        addClickListener(minusClicked -> {
                            Integer amount = amountField.getValue();
                            if (amount < 0) {
                                amountField.setValue(0);
                                ProductsAmountPanel.this.markedProductsSignals.modify(map -> {
                                    if (map == null) {
                                        ProductsAmountPanel.this.markedProductsSignals.set(new LinkedHashMap<>());
                                        return;
                                    }
                                    map.remove(productEntity);
                                });
                            }
                            amountField.setValue(amount - 1);
                        });
                    }}
            );
            amountField.setSuffixComponent(new Button(VaadinIcon.PLUS.create()) {{
                addClickListener(plusClicked -> {
                    Integer amount = amountField.getValue();
                    amountField.setValue(amount + 1);
                });
            }});
            return amountField;
        }

        @Override
        protected VerticalLayout initContent() {
            VerticalLayout verticalLayout = super.initContent();
            verticalLayout.setWidth(200, Unit.PIXELS);
            return verticalLayout;
        }

    }

}
