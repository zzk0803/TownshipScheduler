package zzk.townshipscheduler.ui.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.io.Serializable;
import java.util.Set;

/**
 * DTO for {@link FieldFactoryInfoEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class FactoryProductsDto implements Serializable {

    private static final long serialVersionUID = -4949016055948511264L;

    Long id;

    String category;

    Integer level;

    Set<ProductDto> portfolioGoods;

    /**
     * DTO for {@link ProductEntity}
     */
    @Value
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductDto implements Serializable {

        private static final long serialVersionUID = 2460832027741874293L;

        Long id;

        String name;

        Integer level;

        String bomString;

        String durationString;

        byte[] crawledAsImageImageBytes;

    }

}
