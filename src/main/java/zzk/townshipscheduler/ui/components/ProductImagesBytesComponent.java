package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.WikiCrawledEntity;
import zzk.townshipscheduler.backend.persistence.dao.ProductEntityRepository;
import zzk.townshipscheduler.backend.persistence.dao.WikiCrawledEntityRepository;

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
    public byte[] getProductImage(Long productId) {
        Optional<byte[]> optionalBytes = productEntityRepository.queryProductImageById(productId);
        return optionalBytes.get();
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = {"productImage"},
            key = "#productEntity.id",
            unless = "#result==null"
    )
    public byte[] getProductImage(ProductEntity productEntity) {
        String productEntityName = productEntity.getName();
        Optional<byte[]> optionalBytes = productEntityRepository.queryProductImageByName(productEntityName);
        return optionalBytes.orElse(wikiCrawledEntityRepository.queryEntityBearImageByText(productEntityName).getImageBytes());
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = {"productImage"},
            key = "#productName",
            unless = "#result==null"
    )
    public byte[] getProductImage(String productName) {
        Optional<byte[]> optionalBytes = productEntityRepository.queryProductImageByName(productName);
        return optionalBytes.orElse(wikiCrawledEntityRepository.queryEntityBearImageByText(productName).getImageBytes());
    }

}
