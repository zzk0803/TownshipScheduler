package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.io.ByteArrayInputStream;
import java.util.Map;

public class BillCard extends HorizontalLayout {

    public BillCard(OrderEntity orderEntity) {
        Map<ProductEntity, Integer> productAmountMap = orderEntity.getProductAmountMap();
        productAmountMap.forEach((product, amount) -> {
            Image image = new Image();
            image.setHeight("40px");
            image.setWidth("40px");
//            InputStreamDownloadHandler downloadHandler = DownloadHandler.fromInputStream(downloadEvent -> {
//                ByteArrayInputStream byteArrayInputStream
//                        = new ByteArrayInputStream(product.getCrawledAsImage().getImageBytes());
//                return new DownloadResponse(
//                        byteArrayInputStream,
//                        product.getName(),
//                        "image/webp",
//                        byteArrayInputStream.available()
//                );
//            });
//            image.setSrc(downloadHandler);
            image.setSrc(
                    new StreamResource(
                            product.getName(),
                            () -> new ByteArrayInputStream(product.getCrawledAsImage().getImageBytes())
                    )
            );

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

}
