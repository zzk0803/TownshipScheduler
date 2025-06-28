package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.util.function.Supplier;

public class ProductImage extends Composite<Image> {

    public ProductImage(String productName, Supplier<byte[]> productImageProvider) {
        this(productName, productImageProvider, 30, 30);
    }

    public ProductImage(String productName, Supplier<byte[]> productImageProvider, int weightPx, int heightPx) {
        getContent().setSrc(new StreamResource(
                productName,
                () -> new ByteArrayInputStream(productImageProvider.get())
        ));
        getContent().setHeight(heightPx, Unit.PIXELS);
        getContent().setWidth(weightPx, Unit.PIXELS);
    }

    @Override
    protected Image initContent() {
        return super.initContent();
    }

}
