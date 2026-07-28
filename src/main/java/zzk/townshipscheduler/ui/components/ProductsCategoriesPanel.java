package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
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
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;
import zzk.townshipscheduler.ui.views.product.ProductViewPresenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@Getter
@Setter
public class ProductsCategoriesPanel
        extends Composite<VerticalLayout> {

    private final RadioButtonGroup<FieldFactoryInfoEntity> categoryRadioGroup;

    private final Grid<ProductEntity> productsGrid;

    private final TextField searchField;

    private final ProductViewPresenter productViewPresenter;

    private GridListDataView<ProductEntity> gridListDataView;

    private Set<FieldFactoryInfoEntity> fieldFactoryInfoEntities;

    private FieldFactoryInfoEntity currentSelectFactoryInfo;

    private ProductEntity currentSelectProduct;

    public ProductsCategoriesPanel(ProductViewPresenter productViewPresenter) {
        this.productViewPresenter = productViewPresenter;
        this.setFieldFactoryInfoEntities(this.productViewPresenter.getFieldFactoryInfoCollection());

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
                            setCurrentSelectProduct(null);
                            this.searchField.clear();
                            this.categoryRadioGroup.clear();
                            this.gridListDataView.removeFilters().refreshAll();
                            this.categoryRadioGroup.getDataProvider().refreshAll();
                        }
                )
        );
        searchWrapper.setWidthFull();
        searchWrapper.setSpacing(false);
        searchWrapper.setMargin(false);
        searchWrapper.setPadding(false);
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

    public void filterGoods(String filterCriteria) {
        if (filterCriteria.isBlank()) {
            reset();
            getGridListDataView().refreshAll();
        }

        getGridListDataView().addFilter(
                product -> {
                    String productName = product.getName();
                    String bomString = product.getBomString();
                    return productName.contains(filterCriteria) || bomString.contains(filterCriteria);
                }
        );
    }

    public void reset() {
        this.gridListDataView.removeFilters();
        this.currentSelectFactoryInfo = null;
    }

    private RadioButtonGroup<FieldFactoryInfoEntity> createCategoryRadioGroup() {
        final RadioButtonGroup<FieldFactoryInfoEntity> categoryRadioGroup;
        categoryRadioGroup = new RadioButtonGroup<>();
        categoryRadioGroup.setItemLabelGenerator(FieldFactoryInfoEntity::getCategory);
        categoryRadioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        categoryRadioGroup.setMinWidth("10rem");
        categoryRadioGroup.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        categoryRadioGroup.addValueChangeListener(valueChangeEvent -> {
            FieldFactoryInfoEntity fieldFactoryInfoEntity = valueChangeEvent.getValue();
            this.filterCategoryProduct(fieldFactoryInfoEntity);
        });
        categoryRadioGroup.setItems(this.fieldFactoryInfoEntities);
        return categoryRadioGroup;
    }

    public void filterCategoryProduct(FieldFactoryInfoEntity category) {
        if (Objects.nonNull(category)) {
            setCurrentSelectFactoryInfo(category);
            getGridListDataView().addFilter(product -> {
                Set<ProductManufactureInfoEntity> manufactureInfoEntities = product.getManufactureInfoEntities();
                Set<ProductManufactureInfoEntity> productManufactureInfoSet = category.getProductManufactureInfoSet();
                return manufactureInfoEntities.stream().anyMatch(productManufactureInfoSet::contains);
            });
        }
    }

    private Grid<ProductEntity> createGrid() {
        final Grid<ProductEntity> grid;
        grid = new Grid<>(ProductEntity.class, false);
        grid.setId("goods-categories-grid");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addColumn(ProductEntity::getName)
                .setHeader("Name").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(this::createProductImage))
                .setHeader("Image").setAutoWidth(true);
        grid.addColumn(ProductEntity::getLevel)
                .setHeader("Required Level").setAutoWidth(true);
        grid.addColumn(ProductEntity::getCategory)
                .setHeader("Category").setAutoWidth(true);
        grid.addColumn(ProductEntity::getDurationString)
                .setHeader("Producing Duration").setAutoWidth(true);
        grid.addColumn(ProductEntity::getBomString).setHeader("Materials-String").setAutoWidth(true);
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

    private Component createProductImage(ProductEntity productEntity) {
        byte[] productImage = productViewPresenter.getProductImagesBytesComponent().getProductImage(productEntity);
        return ProductImages.productImage(
                productEntity.getName(),
                productImage
        );
    }

    private Component productMaterialsRender(ProductEntity productEntity) {
        List<ProductManufactureInfoEntity> productManufactureInfoEntities = new ArrayList<>(productEntity.getManufactureInfoEntities());
        if (!productManufactureInfoEntities.isEmpty()) {

            if (productManufactureInfoEntities.size() == 1) {
                HorizontalLayout resultComponent = new HorizontalLayout();
                resultComponent.setAlignItems(FlexComponent.Alignment.CENTER);
                resultComponent.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

                productManufactureInfoEntities.stream()
                        .map(this::mapToMaterialCard)
                        .forEach(resultComponent::add);

                return resultComponent;
            } else {
                HorizontalLayout resultComponent = new HorizontalLayout();
                resultComponent.setAlignItems(FlexComponent.Alignment.CENTER);
                resultComponent.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

                productManufactureInfoEntities.stream()
                        .map(this::mapToMaterialAccordion)
                        .forEach(resultComponent::add);

                return resultComponent;
            }
        } else {
            return new Text(productEntity.getBomString());
        }
    }

    private VerticalLayout mapToMaterialCard(ProductManufactureInfoEntity productManufactureInfoEntity) {
        VerticalLayout materialAmountCard = new VerticalLayout();
        Set<ProductMaterialsRelation> materialsRelationSet = productManufactureInfoEntity.getProductMaterialsRelations();
        materialsRelationSet.forEach(productMaterialsRelation -> {
            ProductEntity material = productMaterialsRelation.getMaterial();
            Integer amount = productMaterialsRelation.getAmount();
            HorizontalLayout materialAmountPair =
                    new HorizontalLayout(
                            createProductImage(material),
                            new Text(" x" + amount)
                    );
            materialAmountPair.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            materialAmountCard.add(materialAmountPair);
        });
        return materialAmountCard;
    }

    private Accordion mapToMaterialAccordion(ProductManufactureInfoEntity productManufactureInfoEntity) {
        Accordion accordion = new Accordion();
        Set<ProductMaterialsRelation> materialsRelationSet = productManufactureInfoEntity.getProductMaterialsRelations();
        materialsRelationSet.forEach(productMaterialsRelation -> {
            ProductEntity material = productMaterialsRelation.getMaterial();
            Integer amount = productMaterialsRelation.getAmount();
            HorizontalLayout materialAmountPair =
                    new HorizontalLayout(
                            createProductImage(material),
                            new Text(" x" + amount)
                    );
            materialAmountPair.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            accordion.add(String.valueOf(productManufactureInfoEntity.getId()), materialAmountPair);
        });
        return accordion;
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout contentLayout = super.initContent();
        contentLayout.setId("container");
        contentLayout.setSizeFull();
        contentLayout.setMargin(false);
        contentLayout.setPadding(false);
        contentLayout.setSpacing(false);
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
        this.forceReloadingData();
    }

    public void forceReloadingData() {
        setGridListDataView(
                getProductsGrid().setItems(
                        getFieldFactoryInfoEntities().stream()
                                .flatMap(fieldFactoryInfoEntity -> fieldFactoryInfoEntity.getProductManufactureInfoSet().stream().map(ProductManufactureInfoEntity::getProductEntity))
                                .toList()
                )
        );
        this.productsGrid.getDataProvider().refreshAll();
        this.categoryRadioGroup.getDataProvider().refreshAll();
    }

}
