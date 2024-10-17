package zzk.project.vaadinproject.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import lombok.Setter;
import zzk.project.vaadinproject.backend.persistence.Goods;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.function.Consumer;

@Getter
@Setter
@SpringComponent
@UIScope
public class GoodsCategoriesPanel
        extends Composite<VerticalLayout> {

    private final ListBox<String> categoryListBox;

    private final GoodsCategoriesPanelPresenter presenter;

    private final Grid<Goods> goodsGrid;

    private final TextField searchField;

    private transient Goods selectedGoods;

    public GoodsCategoriesPanel(GoodsCategoriesPanelPresenter presenter) {
        this.presenter = presenter;
        this.presenter.setGroupByCategoryGrid(this);

        searchField = createSearchField();
        categoryListBox = createCategoiesListBox();
        goodsGrid = createGrid();

        var scrollerForListBox = new Scroller();
        scrollerForListBox.setContent(categoryListBox);

        HorizontalLayout wrapper = new HorizontalLayout();
        wrapper.setId("wrapper");
        wrapper.setSizeFull();
        wrapper.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.STRETCH);
        wrapper.add(scrollerForListBox);
        wrapper.setFlexShrink(0, scrollerForListBox);
        wrapper.add(goodsGrid);

        getContent().add(searchField);
        getContent().addAndExpand(wrapper);
    }

    private TextField createSearchField() {
        TextField textField = new TextField();
        textField.setPlaceholder("Search...");
        textField.setValueChangeMode(ValueChangeMode.LAZY);
        textField.setWidthFull();
        textField.addValueChangeListener(valueChange -> {
            String criteria = valueChange.getValue();
            getPresenter().filterGoods(criteria);
            getPresenter().filterCategories(criteria, this.categoryListBox);
        });
        return textField;
    }

    private ListBox<String> createCategoiesListBox() {
        final ListBox<String> categoryListBox;
        categoryListBox = new ListBox<>();
        categoryListBox.setMinWidth("10rem");
        categoryListBox.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        categoryListBox.addValueChangeListener(valueChangeEvent -> {
            String category = valueChangeEvent.getValue();
            getPresenter().categoryGoods(category);
        });
        return categoryListBox;
    }

    private Grid<Goods> createGrid() {
        final Grid<Goods> grid;
        grid = new Grid<>(Goods.class, false);
        grid.setId("goods-categories-grid");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addColumn(Goods::getCategory).setHeader("Category").setAutoWidth(true);
        grid.addColumn(Goods::getName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(Goods::getLevel).setHeader("Required Level").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(this::goodsImageRender)).setHeader("Image").setAutoWidth(true);
        grid.addColumn(Goods::getBomString).setHeader("Materials").setAutoWidth(true);
        grid.addColumn(Goods::getDurationString).setHeader("Producing Duration").setAutoWidth(
                true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setHeightFull();
        grid.asSingleSelect().addValueChangeListener(gridGoodsVoValueChangeEvent -> {
            gridGoodsVoValueChangeEvent.getHasValue().getOptionalValue().ifPresentOrElse(goods -> {
                getPresenter().setOptionalSelectedGoods(Optional.of(goods));
            }, () -> {
                getPresenter().setOptionalSelectedGoods(Optional.empty());
            });
        });
        return grid;
    }

    private Component goodsImageRender(Goods goods) {
        Component component = null;
        String name = goods.getName();
        Optional<byte[]> imageBytesOptional = Optional.ofNullable(goods.getImageBytes());
        if (imageBytesOptional.isPresent()) {
            component = new Image(
                    new StreamResource(name, () -> new ByteArrayInputStream(imageBytesOptional.get())),
                    name
            );
        } else {
            component = new Button(VaadinIcon.REFRESH.create(), click -> {
                presenter.refreshImgBtnClick(goods);
            });
        }
        return component;
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout contentLayout = super.initContent();
        contentLayout.setId("container");
        contentLayout.setSizeFull();
        return contentLayout;
    }

    public void consumeSelected(Consumer<Goods> consumer) {
        presenter.getOptionalSelectedGoods().ifPresent(consumer);
    }

    public void refreshImgBtnClickDone(Goods goods) {
        UI.getCurrent().access(() -> {
            goodsGrid.getGenericDataView().refreshItem(goods);
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        getPresenter().queryAndCacheGoods();
        getPresenter().setupGridItems(getGoodsGrid());
        getPresenter().setupCategoriesItems(getCategoryListBox());
    }

}
