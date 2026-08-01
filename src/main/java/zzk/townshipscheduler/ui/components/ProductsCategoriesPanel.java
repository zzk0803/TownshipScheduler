package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.ui.views.product.ProductViewPresenter;

import java.util.Comparator;
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
                            this.gridListDataView.removeFilters()
                                    .refreshAll();
                            this.categoryRadioGroup.getDataProvider()
                                    .refreshAll();
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
        categoryRadioGroup.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)");
        categoryRadioGroup.addValueChangeListener(valueChangeEvent -> {
            FieldFactoryInfoEntity fieldFactoryInfoEntity = valueChangeEvent.getValue();
            this.currentSelectFactoryInfoValueSignal.set(fieldFactoryInfoEntity);
        });
        categoryRadioGroup.setItems(this.fieldFactoryInfoEntities);
        return categoryRadioGroup;
    }

    private Grid<ProductEntity> createGrid() {
        final Grid<ProductEntity> grid = new ProductsGrid(this.productViewPresenter.getProductEntityImageFunction(), this.productViewPresenter.getMaterialsRenderFunction());
        grid.setHeightFull();

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
                        this.gridListDataView = grid.setItems(fieldFactoryInfoEntity.getProductEntities()
                                .stream()
                                .sorted(Comparator.comparingInt(ProductEntity::getLevel)
                                        .thenComparing(ProductEntity::getName))
                                .toList());
                    } else {
                        this.gridListDataView = grid.setItems(
                                getFieldFactoryInfoEntities().stream()
                                        .flatMap(fieldFactoryInfo -> fieldFactoryInfo.getProductEntities()
                                                .stream())
                                        .sorted(Comparator.comparingInt(ProductEntity::getLevel)
                                                .thenComparing(ProductEntity::getName))
                                        .toList()
                        );
                    }
                }
        );
        return grid;
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
                        String productName = product.getName()
                                .toLowerCase();
                        String bomString = product.getBomString()
                                .toLowerCase();
                        return productName.contains(filterCriteria.toLowerCase()) || bomString.contains(filterCriteria.toLowerCase());
                    }
            );
        }
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

}
