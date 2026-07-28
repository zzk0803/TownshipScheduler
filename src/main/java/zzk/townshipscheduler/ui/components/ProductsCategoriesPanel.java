package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;
import zzk.townshipscheduler.ui.views.product.ProductViewPresenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    private ValueSignal<String> filterCriterialValueSignal = new ValueSignal<>("");

    private ValueSignal<FieldFactoryInfoEntity> currentSelectFactoryInfoValueSignal = new ValueSignal<>(null);

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
            this.filterCriterialValueSignal.set(criteria);
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
            FieldFactoryInfoEntity fieldFactoryInfoEntity = valueChangeEvent.getValue();
            this.currentSelectFactoryInfoValueSignal.set(fieldFactoryInfoEntity);
        });
        categoryRadioGroup.setItems(this.fieldFactoryInfoEntities);
        return categoryRadioGroup;
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
        grid.addColumn(new ComponentRenderer<>(this::productMaterialsRender))
                .setHeader("Materials").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setHeightFull();
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        Signal.effect(
                grid,
                () -> {
                    String criterial = filterCriterialValueSignal.get();
                    this.filterProducts(criterial);
                }
        );
        Signal.effect(
                grid,
                () -> {
                    FieldFactoryInfoEntity fieldFactoryInfoEntity = currentSelectFactoryInfoValueSignal.get();
                    if (fieldFactoryInfoEntity != null) {
                        this.gridListDataView = grid.setItems(fieldFactoryInfoEntity.getProductManufactureInfoSet().stream().map(ProductManufactureInfoEntity::getProductEntity).toList());
                    } else {
                        this.gridListDataView = grid.setItems(
                                getFieldFactoryInfoEntities().stream()
                                        .flatMap(fieldFactoryInfo -> fieldFactoryInfo.getProductManufactureInfoSet().stream().map(ProductManufactureInfoEntity::getProductEntity))
                                        .toList()
                        );
                    }
                }
        );
        return grid;
    }

    private Component createProductImage(ProductEntity productEntity) {
        byte[] productImage = productEntity.getCrawledAsImage().getImageBytes();
        return ProductImages.productImage(
                productEntity.getName(),
                productImage
        );
    }

    public void filterProducts(String filterCriteria) {
        if (filterCriteria == null) {
            if (this.gridListDataView != null) {
                this.gridListDataView.removeFilters();
                this.gridListDataView.refreshAll();
            }
            return;
        }

        if (filterCriteria.isBlank()) {
            if (this.gridListDataView != null) {
                this.gridListDataView.removeFilters();
                this.gridListDataView.refreshAll();
            }
            return;
        }

        if (this.gridListDataView != null) {
            this.gridListDataView.addFilter(
                    product -> {
                        String productName = product.getName().toLowerCase();
                        String bomString = product.getBomString().toLowerCase();
                        return productName.contains(filterCriteria.toLowerCase()) || bomString.contains(filterCriteria.toLowerCase());
                    }
            );
        }
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

    public void reset() {
        if (this.gridListDataView != null) {
            this.gridListDataView.removeFilters();
        }
        this.currentSelectFactoryInfoValueSignal.set(null);
        this.filterCriterialValueSignal.set(null);
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

    public void refreshImgBtnClickDone(ProductEntity productEntity) {
        UI.getCurrent().access(() -> {
            productsGrid.getGenericDataView().refreshItem(productEntity);
        });
    }

}
