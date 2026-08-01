package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.parameters.P;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;

import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

@UtilityClass
public class ProductImages {

    public static DownloadHandler productImageDownloadHandler(ProductEntity productEntity) {
        Objects.requireNonNull(productEntity.getName());
        Objects.requireNonNull(productEntity.getCrawledAsImage());
        return createDownloadHandlerOfProductImage(productEntity.getName(), productEntity.getCrawledAsImage().getImageBytes());
    }

    private static @NonNull InputStreamDownloadHandler createDownloadHandlerOfProductImage(String productName, byte[] bytes) {
        return DownloadHandler.fromInputStream(
                _ -> new DownloadResponse(
                        new ByteArrayInputStream(bytes),
                        productName,
                        "application/octet-stream",
                        bytes.length
                )
        );
    }

    public static Image productImage(String productName, Supplier<byte[]> bytesSupplier) {
        return productImage(productName, bytesSupplier.get());
    }

    public static Image productImage(String productName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new Image("images/placeholder.png", "placeholder");
        }

        return new Image(
                createDownloadHandlerOfProductImage(productName, bytes),
                productName
        );
    }

    public static Image productImage(ProductEntity product, Function<ProductEntity, byte[]> productImageBytesFunction) {
        return productImage(product.getName(), productImageBytesFunction.apply(product));
    }

}
