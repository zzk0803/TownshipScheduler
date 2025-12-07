package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ProductImages {

    public static Image productImage(String productName, WikiCrawledEntity crawledEntity) {
        if (crawledEntity == null) {
            return new Image("images/placeholder.png", "placeholder");
        }
        return productImage(productName, crawledEntity.getImageBytes());
    }

    public static Image productImage(String productName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new Image("images/placeholder.png", "placeholder");
        }

        return new Image(
                DownloadHandler.fromInputStream(
                        _ -> new DownloadResponse(
                                new ByteArrayInputStream(bytes),
                                productName,
                                "application/octet-stream",
                                bytes.length
                        )
                ),
                productName
        );
    }

    public static Image productImage(String productName, Supplier<byte[]> bytesSupplier) {
        return productImage(productName, bytesSupplier.get());
    }

    public static CompletableFuture<Image> productImage(String productName, CompletableFuture<byte[]> bytesFuture) {
        return bytesFuture.thenApply(bytes -> productImage(productName, bytes));
    }

}
