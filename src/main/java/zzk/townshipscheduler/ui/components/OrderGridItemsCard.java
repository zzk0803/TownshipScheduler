package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class OrderGridItemsCard extends HorizontalLayout {

    public Function<ProductEntity, byte[]> productImageFunction
            = productEntity -> productEntity.getCrawledAsImage().getImageBytes();

    public OrderGridItemsCard(OrderEntity orderEntity) {
        Map<ProductEntity, Integer> productAmountMap = orderEntity.getProductAmountMap();
        productAmountMap.forEach((product, amount) -> {
            Image image = ProductImages.productImage(
                    product.getName(),
                    productImageFunction.apply(product)
            );
            image.setHeight("40px");
            image.setWidth("40px");

            Span span = new Span(product.getName());

            VerticalLayout leftImageAndTextVL = new VerticalLayout(image, span);
            leftImageAndTextVL.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

            HorizontalLayout itemAndAmountHL = new HorizontalLayout(leftImageAndTextVL, new Span("x" + amount));
            itemAndAmountHL.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            itemAndAmountHL.setSpacing(false);
            itemAndAmountHL.setMargin(false);
            add(itemAndAmountHL);
        });
    }

    public OrderGridItemsCard withProductImageFunction(Function<ProductEntity, byte[]> productImageFunction) {
        this.productImageFunction = productImageFunction;
        return this;
    }

}
