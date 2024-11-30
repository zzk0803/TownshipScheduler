package zzk.townshipscheduler.pojo.projection;

import lombok.Value;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

/**
 * DTO for {@link ProductEntity}
 */
@Value
public class ProductEntityDto {

    Long id;

    String name;

    String nameForMaterial;

    String category;

    String bomString;

    String durationString;

}
