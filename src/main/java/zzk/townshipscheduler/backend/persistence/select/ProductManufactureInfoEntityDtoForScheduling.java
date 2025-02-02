package zzk.townshipscheduler.backend.persistence.select;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.Set;

/**
 * DTO for {@link ProductManufactureInfoEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductManufactureInfoEntityDtoForScheduling implements Serializable {

    @Serial
    private static final long serialVersionUID = 76673058680837411L;

     ProductEntityDtoJustId productEntity;

     Duration producingDuration;

     Set<ProductMaterialsRelationDtoForScheduling> productMaterialsRelations;

}
