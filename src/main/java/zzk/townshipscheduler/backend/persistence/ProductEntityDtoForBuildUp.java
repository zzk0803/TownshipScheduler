package zzk.townshipscheduler.backend.persistence;

import lombok.Value;

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
