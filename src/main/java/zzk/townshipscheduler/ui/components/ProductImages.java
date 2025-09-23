package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

import java.io.ByteArrayInputStream;

public class ProductImages {

    public static Image productImage(String productName, byte[] bytes) {
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

}
