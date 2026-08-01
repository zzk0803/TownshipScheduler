package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.RequiredArgsConstructor;
import org.hibernate.LazyInitializationException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntityRepository;

import java.util.Optional;

@SpringComponent
@RequiredArgsConstructor
public class ProductImagesBytesComponent {

    private final ProductEntityRepository productEntityRepository;

    private final WikiCrawledEntityRepository wikiCrawledEntityRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = {"productImage"},
            key = "#productId",
            unless = "#result==null"
    )
    public byte[] fetchProductImageBytes(Long productId) {
        Optional<byte[]> optionalBytes = productEntityRepository.queryProductImageById(productId);
        return optionalBytes.get();
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = {"productImage"},
            key = "#productEntity.id",
            unless = "#result==null"
    )
    public byte[] fetchProductImageBytes(ProductEntity productEntity) {
        try {
            return productEntity.getCrawledAsImage()
                    .getImageBytes();
        } catch (LazyInitializationException lie) {
            String productEntityName = productEntity.getName();
            Optional<byte[]> optionalBytes = productEntityRepository.queryProductImageByName(productEntityName);
            return optionalBytes.orElse(wikiCrawledEntityRepository.queryImageBytesByText(productEntityName));
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = {"productImage"},
            key = "#productName",
            unless = "#result==null"
    )
    public byte[] fetchProductImageBytes(String productName) {
        Optional<byte[]> optionalBytes = productEntityRepository.queryProductImageByName(productName);
        return optionalBytes.orElse(wikiCrawledEntityRepository.queryImageBytesByText(productName));
    }

}
