package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;

import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@Getter
@Setter
public class ProductsCategoriesPanel extends Composite<VerticalLayout> {

    private final RadioButtonGroup<FieldFactoryInfoEntity> categoryRadioGroup;

    private final Grid<ProductEntity> productsGrid;

    private final TextField searchField;

    private GridListDataView<ProductEntity> gridListDataView;

    private Set<ProductEntity> productEntities;

    private List<FieldFactoryInfoEntity> factoryList;

    private FieldFactoryInfoEntity currentSelectFactoryInfo;

    private ProductEntity currentSelectProduct;

    public ProductsCategoriesPanel(Set<ProductEntity> productEntities) {
        setProductEntities(productEntities);
        setFactoryList(
                getProductEntities().stream()
                        .map(ProductEntity::getFieldFactoryInfo)
                        .distinct()
                        .sorted(Comparator.comparing(FieldFactoryInfoEntity::getLevel, Integer::compareTo))
                        .toList()
        );

        searchField = createSearchField();
        categoryRadioGroup = createCategoryRadioGroup();
        productsGrid = createGrid();

        Scroller scrollerForListBox = new Scroller();
        scrollerForListBox.setContent(this.categoryRadioGroup);

        HorizontalLayout wrapper = new HorizontalLayout();
        wrapper.setId("wrapper");
        wrapper.setSizeFull();
        wrapper.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.STRETCH);
        wrapper.add(scrollerForListBox);
        wrapper.setFlexShrink(0, scrollerForListBox);
        wrapper.add(productsGrid);

        HorizontalLayout searchWrapper = new HorizontalLayout(
                searchField,
                new Button(
                        VaadinIcon.REFRESH.create(),
                        buttonClickEvent -> {
                            getGridListDataView().removeFilters();
                            setCurrentSelectProduct(null);
                            searchField.clear();
                            categoryRadioGroup.clear();
                            productsGrid.getDataProvider().refreshAll();
                            categoryRadioGroup.getDataProvider().refreshAll();
                        }
                )
        );
        searchWrapper.setWidthFull();
        searchWrapper.getFlexGrow(searchField);
        getContent().add(searchWrapper);
        getContent().addAndExpand(wrapper);
    }

    private TextField createSearchField() {
        TextField textField = new TextField();
        textField.setPlaceholder("Search...");
        textField.setValueChangeMode(ValueChangeMode.LAZY);
        textField.setWidthFull();
        textField.addValueChangeListener(valueChange -> {
            String criteria = valueChange.getValue();
            this.filterGoods(criteria);
        });
        return textField;
    }

    private RadioButtonGroup<FieldFactoryInfoEntity> createCategoryRadioGroup() {
        final RadioButtonGroup<FieldFactoryInfoEntity> categoryRadioGroup;
        categoryRadioGroup = new RadioButtonGroup<>();
        categoryRadioGroup.setItemLabelGenerator(FieldFactoryInfoEntity::getCategory);
        categoryRadioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        categoryRadioGroup.setMinWidth("10rem");
        categoryRadioGroup.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        categoryRadioGroup.addValueChangeListener(valueChangeEvent -> {
            this.filterCategoryProduct(valueChangeEvent.getValue());
        });
        categoryRadioGroup.setItems(this.factoryList);
        return categoryRadioGroup;
    }

    private Grid<ProductEntity> createGrid() {
        final Grid<ProductEntity> grid;
        grid = new Grid<>(ProductEntity.class, false);
        grid.setId("goods-categories-grid");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addColumn(ProductEntity::getName)
                .setHeader("Name").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(this::goodsImageRender))
                .setHeader("Image").setAutoWidth(true);
        grid.addColumn(ProductEntity::getLevel)
                .setHeader("Required Level").setAutoWidth(true);
        grid.addColumn(ProductEntity::getCategory)
                .setHeader("Category").setAutoWidth(true);
//        grid.addColumn(ProductEntity::getBomString).setHeader("Materials-String").setAutoWidth(true)
        grid.addColumn(ProductEntity::getDurationString)
                .setHeader("Producing Duration").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(this::productMaterialsRender))
                .setHeader("Materials").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setHeightFull();
        grid.asSingleSelect().addValueChangeListener(valueChangeEvent -> {
            valueChangeEvent.getHasValue().getOptionalValue().ifPresentOrElse(
                    this::setCurrentSelectProduct,
                    () -> this.setCurrentSelectProduct(null)
            );
        });
        return grid;
    }

    public void filterGoods(String filterCriteria) {
        if (filterCriteria.isBlank()) {
            reset();
            getGridListDataView().refreshAll();
        }

        getGridListDataView().addFilter(product -> {
                    String productName = product.getName();
                    return productName.contains(filterCriteria) || filterCriteria.contains(productName);
                }
        );
    }

    public void filterCategoryProduct(FieldFactoryInfoEntity category) {
        if (Objects.nonNull(category)) {
            setCurrentSelectFactoryInfo(category);
            getGridListDataView().addFilter(product -> getCurrentSelectFactoryInfo().equals(product.getFieldFactoryInfo()));
        }
    }

    private Component goodsImageRender(ProductEntity productEntity) {
        Component component = null;
        String name = productEntity.getName();
        byte[] imageBytes = productEntity.getCrawledAsImage().getImageBytes();
        component = new Image(
                new StreamResource(name, () -> new ByteArrayInputStream(imageBytes)),
                name
        );

        return component;
    }

    private Component productMaterialsRender(ProductEntity productEntity) {
        Set<ProductManufactureInfoEntity> productManufactureInfoEntities = productEntity.getManufactureInfoEntities();
        if (productManufactureInfoEntities != null && !productManufactureInfoEntities.isEmpty()) {
            HorizontalLayout resultComponent = new HorizontalLayout();
            resultComponent.setAlignItems(FlexComponent.Alignment.CENTER);
            resultComponent.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

            productManufactureInfoEntities.stream()
                    .map(this::mapToMaterialCard)
                    .forEach(resultComponent::add);

            return resultComponent;
        } else {
            return new Text(productEntity.getBomString());
        }
    }

    public void reset() {
        this.gridListDataView.removeFilters();
        this.currentSelectFactoryInfo = null;
    }

    private VerticalLayout mapToMaterialCard(ProductManufactureInfoEntity productManufactureInfoEntity) {
        VerticalLayout materialAmountCard = new VerticalLayout();
        Set<ProductMaterialsRelation> materialsRelationSet = productManufactureInfoEntity.getProductMaterialsRelations();
        materialsRelationSet.forEach(productMaterialsRelation -> {
            ProductEntity material = productMaterialsRelation.getMaterial();
            Integer amount = productMaterialsRelation.getAmount();
            HorizontalLayout materialAmountPair =
                    new HorizontalLayout(
                            goodsImageRender(material),
                            new Text(" x" + amount)
                    );
            materialAmountPair.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            materialAmountCard.add(materialAmountPair);
        });
        return materialAmountCard;
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout contentLayout = super.initContent();
        contentLayout.setId("container");
        contentLayout.setSizeFull();
        contentLayout.setMargin(false);
        return contentLayout;
    }

    public void consumeSelected(Consumer<ProductEntity> consumer) {
        consumer.accept(getCurrentSelectProduct());
    }

    public void refreshImgBtnClickDone(ProductEntity productEntity) {
        UI.getCurrent().access(() -> {
            productsGrid.getGenericDataView().refreshItem(productEntity);
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setGridListDataView(getProductsGrid().setItems(getProductEntities()));
        this.forceReloadingData();
    }

    public void forceReloadingData() {
        this.productsGrid.getDataProvider().refreshAll();
        this.categoryRadioGroup.getDataProvider().refreshAll();
    }

}
