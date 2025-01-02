package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;


@SpringComponent
@UIScope
public class GoodsCategoriesPanel extends Composite<VerticalLayout> {

    private final RadioButtonGroup<FieldFactoryInfoEntity> categoryRadioGroup;

    private final GoodsCategoriesPanelPresenter presenter;

    private final Grid<ProductEntity> goodsGrid;

    private final TextField searchField;

    public GoodsCategoriesPanel(GoodsCategoriesPanelPresenter presenter) {
        this.presenter = presenter;
        this.presenter.setGroupByCategoryGrid(this);
        this.presenter.queryAndCache();

        searchField = createSearchField(this.presenter);
        categoryRadioGroup = createCategoryRadioGroup(this.presenter);
        goodsGrid = createGrid(this.presenter);

        Scroller scrollerForListBox = new Scroller();
        scrollerForListBox.setContent(this.categoryRadioGroup);

        HorizontalLayout wrapper = new HorizontalLayout();
        wrapper.setId("wrapper");
        wrapper.setSizeFull();
        wrapper.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.STRETCH);
        wrapper.add(scrollerForListBox);
        wrapper.setFlexShrink(0, scrollerForListBox);
        wrapper.add(goodsGrid);

        HorizontalLayout searchWrapper = new HorizontalLayout(
                searchField,
                new Button(
                        VaadinIcon.REFRESH.create(),
                        buttonClickEvent -> {
                            presenter.reset();
                            searchField.clear();
                            categoryRadioGroup.clear();
                            goodsGrid.getDataProvider().refreshAll();
                            categoryRadioGroup.getDataProvider().refreshAll();
                        }
                )
        );
        searchWrapper.setWidthFull();
        searchWrapper.getFlexGrow(searchField);
        getContent().add(searchWrapper);
        getContent().addAndExpand(wrapper);
    }

    private TextField createSearchField(GoodsCategoriesPanelPresenter presenter) {
        TextField textField = new TextField();
        textField.setPlaceholder("Search...");
        textField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        textField.setWidthFull();
        textField.addValueChangeListener(valueChange -> {
            String criteria = valueChange.getValue();
            presenter.filterGoods(criteria);
        });
        return textField;
    }

    private RadioButtonGroup<FieldFactoryInfoEntity> createCategoryRadioGroup(GoodsCategoriesPanelPresenter presenter) {
        final RadioButtonGroup<FieldFactoryInfoEntity> categoryRadioGroup;
        categoryRadioGroup = new RadioButtonGroup<>();
        categoryRadioGroup.setItemLabelGenerator(FieldFactoryInfoEntity::getCategory);
        categoryRadioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        categoryRadioGroup.setMinWidth("10rem");
        categoryRadioGroup.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        categoryRadioGroup.addValueChangeListener(valueChangeEvent -> {
            presenter.filterCategoryProduct(valueChangeEvent.getValue());
        });
        presenter.setupCategoriesItems(categoryRadioGroup);
        return categoryRadioGroup;
    }

    private Grid<ProductEntity> createGrid(GoodsCategoriesPanelPresenter presenter) {
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
        grid.asSingleSelect().addValueChangeListener(gridGoodsVoValueChangeEvent -> {
            gridGoodsVoValueChangeEvent.getHasValue().getOptionalValue().ifPresentOrElse(
                    goods -> {
                        this.presenter.setCurrentSelect(Optional.of(goods));
                    }, () -> {
                        this.presenter.setCurrentSelect(Optional.empty());
                    }
            );
        });
        presenter.setupGridItems(grid);
        return grid;
    }

    private Component goodsImageRender(ProductEntity productEntity) {
        Component component = null;
        String name = productEntity.getName();
        Optional<byte[]> imageBytesOptional = Optional.ofNullable(productEntity.getCrawledAsImage().getImageBytes());
//        Optional<byte[]> imageBytesOptional = Optional.ofNullable(productEntity.getImageBytes());
        if (imageBytesOptional.isPresent()) {
            component = new Image(
                    new StreamResource(name, () -> new ByteArrayInputStream(imageBytesOptional.get())),
                    name
            );
        } else {
            component = new Button(
                    VaadinIcon.REFRESH.create(),
                    click -> presenter.refreshImgBtnClick(productEntity)
            );
        }

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
        return contentLayout;
    }

    public void consumeSelected(Consumer<ProductEntity> consumer) {
        presenter.getCurrentSelect().ifPresent(consumer);
    }

    public void refreshImgBtnClickDone(ProductEntity productEntity) {
        UI.getCurrent().access(() -> {
            goodsGrid.getGenericDataView().refreshItem(productEntity);
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.forceReloadingData();
    }

    public void forceReloadingData() {
        this.presenter.queryAndCache();
        this.goodsGrid.getDataProvider().refreshAll();
        this.categoryRadioGroup.getDataProvider().refreshAll();
    }

}
