package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.ui.views.product.ProductViewPresenter;

import javax.swing.*;

public class ProductDetailArticle extends ViewportLayout {

    private final ProductEntity productEntity;

    private final ProductViewPresenter productViewPresenter;

    public ProductDetailArticle(ProductEntity productEntity, ProductViewPresenter productViewPresenter) {
        this.productEntity = productEntity;
        this.productViewPresenter = productViewPresenter;

        buildProductReadonlyForm(productEntity);
    }

    private Object buildProductReadonlyForm(ProductEntity productEntity) {
        FormLayout basicInfoReadonlyForm = new FormLayout();

        TextField nameField = new TextField();
        nameField.setValue(productEntity.getName());
        nameField.setReadOnly(true);
        basicInfoReadonlyForm.addFormItem(nameField, "Name");

        TextField categoryField = new TextField();
        categoryField.setValue(productEntity.getCategory());
        categoryField.setReadOnly(true);
        basicInfoReadonlyForm.addFormItem(categoryField, "Category");

        TextField levelField = new TextField();
        levelField.setValue(String.valueOf(productEntity.getLevel()));
        levelField.setReadOnly(true);
        basicInfoReadonlyForm.addFormItem(levelField, "Level");

        TextField costField = new TextField();
        costField.setValue(String.valueOf(productEntity.getCost()));
        costField.setReadOnly(true);
        basicInfoReadonlyForm.addFormItem(costField, "Cost");

        TextField sellPriceField = new TextField();
        sellPriceField.setValue(String.valueOf(productEntity.getSellPrice()));
        sellPriceField.setReadOnly(true);
        basicInfoReadonlyForm.addFormItem(sellPriceField, "SellPrice");

        TextField xpField = new TextField();
        xpField.setValue(String.valueOf(productEntity.getXp()));
        xpField.setReadOnly(true);
        basicInfoReadonlyForm.addFormItem(xpField, "Xp");

        productViewPresenter.getMaterials(productEntity);
        productViewPresenter.getComposite(productEntity);
        return null;
    }

}
