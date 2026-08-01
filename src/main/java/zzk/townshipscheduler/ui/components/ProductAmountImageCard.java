package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.util.function.Function;

public class ProductAmountImageCard
        extends VerticalLayout {

    public ProductAmountImageCard(ProductEntity productEntity, int amount, Function<ProductEntity, Image> productEntityImageFunction) {
        setMargin(false);
        setSpacing(false);

        add(productEntityImageFunction.apply(productEntity), new Span(productEntity.getName()+" x "+amount));
    }

}
