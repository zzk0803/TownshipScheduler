package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.ui.views.product.ProductView;
import zzk.townshipscheduler.ui.views.product.ProductViewPresenter;

public class ProductDetailPanel
        extends Composite<VerticalLayout> {

    private final ProductEntity productEntity;

    private final ProductViewPresenter productViewPresenter;

    public ProductDetailPanel(ProductEntity productEntity, ProductViewPresenter productViewPresenter) {
        this.productEntity = productEntity;
        this.productViewPresenter = productViewPresenter;

        Button backwardButton = new Button(
                VaadinIcon.ARROW_CIRCLE_LEFT_O.create()
        );
        backwardButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.PRIMARY);
        backwardButton.addClickListener(_ -> UI.getCurrent()
                .navigate(ProductView.class));
        getContent().add(backwardButton);
        getContent().add(buildProductReadonlyForm(productEntity));

        Grid<ProductEntity> materialsGrid = new ProductsBriefGrid(this.productViewPresenter.getProductEntityImageFunction());
        materialsGrid.setItems(this.productViewPresenter.getMaterials(productEntity));
        materialsGrid.setEmptyStateText("No Materials");
        getContent().add(materialsGrid);

        Grid<ProductEntity> compositeGrid = new ProductsBriefGrid(this.productViewPresenter.getProductEntityImageFunction());
        compositeGrid.setItems(this.productViewPresenter.getComposite(productEntity));
        compositeGrid.setEmptyStateText("No Composites");
        getContent().add(compositeGrid);
    }

    private Component buildProductReadonlyForm(ProductEntity productEntity) {
        FormLayout productDetailLayout = new FormLayout();

        TextField nameField = new TextField("Name");
        nameField.setValue(productEntity.getName());
        nameField.setReadOnly(true);

        TextField categoryField = new TextField("Category");
        categoryField.setValue(productEntity.getCategory());
        categoryField.setReadOnly(true);

        TextField levelField = new TextField("Level");
        levelField.setValue(String.valueOf(productEntity.getLevel()));
        levelField.setReadOnly(true);

        TextField costField = new TextField("Cost");
        costField.setValue(String.valueOf(productEntity.getCost()));
        costField.setReadOnly(true);

        TextField sellPriceField = new TextField("SellPrice");
        sellPriceField.setValue(String.valueOf(productEntity.getSellPrice()));
        sellPriceField.setReadOnly(true);

        TextField xpField = new TextField("Xp");
        xpField.setValue(String.valueOf(productEntity.getXp()));
        xpField.setReadOnly(true);

        TextField dealerValueField = new TextField("DealerValue");
        dealerValueField.setValue(String.valueOf(productEntity.getDealerValue()));
        dealerValueField.setReadOnly(true);

        TextField helpValueField = new TextField("helpValue");
        helpValueField.setValue(String.valueOf(productEntity.getHelpValue()));
        helpValueField.setReadOnly(true);

        productDetailLayout.add(nameField, categoryField, levelField, costField, sellPriceField, xpField, dealerValueField, helpValueField);
        return productDetailLayout;
    }

}
