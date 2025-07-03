package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import zzk.townshipscheduler.backend.persistence.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ProductDetailPanel extends Composite<VerticalLayout> {

    private ProductEntity productEntity;

    public ProductDetailPanel(ProductEntity productEntity) {
        this.productEntity = productEntity;
        String name = productEntity.getName();
        WikiCrawledEntity crawledAsImage = productEntity.getCrawledAsImage();
        String category = productEntity.getCategory();
        FieldFactoryInfoEntity fieldFactoryInfo = productEntity.getFieldFactoryInfo();
        Integer level = productEntity.getLevel();
        Integer cost = productEntity.getCost();
        Integer sellPrice = productEntity.getSellPrice();
        Set<ProductManufactureInfoEntity> manufactureInfoEntities = productEntity.getManufactureInfoEntities();
    }

    private Object createProductMaterialWrapper() {
        Collection<ProductMaterialsRelation> productMaterials = List.of();

        return new Object();
    }

    private Object createProductDetailWrapper() {
        HorizontalLayout wrapper = new HorizontalLayout();
        VerticalLayout textWrapper = new VerticalLayout();

        TextField name = new TextField("Name");
        Image image = new Image();
        TextField category = new TextField("Category");

        textWrapper.add(name,category);

        wrapper.add(image, textWrapper);
        return new Object();
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout verticalLayout = super.initContent();
        verticalLayout.setHeightFull();
        verticalLayout.setMargin(false);
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        verticalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        return verticalLayout;
    }

}
