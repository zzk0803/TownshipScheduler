package zzk.townshipscheduler.pojo.projection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;

import java.io.Serializable;
import java.time.Duration;
import java.util.Set;

/**
 * DTO for {@link ProductManufactureInfoEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductManufactureInfoEntityForSchedulingDto implements Serializable {

    Long id;

    Long productEntityId;

    Set<ProductMaterialsRelationForSchedulingDto> materialsRelationSet;

    Duration producingDuration;

}
