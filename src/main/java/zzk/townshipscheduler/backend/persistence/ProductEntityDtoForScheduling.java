package zzk.townshipscheduler.backend.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * DTO for {@link ProductEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEntityDtoForScheduling implements Serializable {

    @Serial
    private static final long serialVersionUID = -6832919164990712660L;

     Long id;

     String name;

     Integer level;

     Set<ProductManufactureInfoEntityDtoForScheduling> manufactureInfoEntities;

    FieldFactoryInfoEntityDtoJustId fieldFactoryInfo;

}
