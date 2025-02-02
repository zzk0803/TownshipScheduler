package zzk.townshipscheduler.backend.persistence.select;

import lombok.Value;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

/**
 * DTO for {@link ProductEntity}
 */
@Value
public class ProductEntityDtoForBuildUp {

    Long id;

    String name;

    String nameForMaterial;

    String category;

    String bomString;

    String durationString;

}
