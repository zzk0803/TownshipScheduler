package zzk.townshipscheduler.pojo.projection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.io.Serializable;
import java.util.Set;

/**
 * DTO for {@link zzk.townshipscheduler.backend.persistence.ProductEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEntityForSchedulingDto implements Serializable {

    Long id;

    String name;

    String category;

    FieldFactoryInfoEntityForSchedulingDto fieldFactoryInfo;

    Integer level;

    Integer gainWhenCompleted;

    String bomString;

    String durationString;

    Set<ProductManufactureInfoEntityForSchedulingDto> manufactureInfoEntitySet;

}
