package zzk.townshipscheduler.pojo.projection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;

import java.io.Serializable;

/**
 * DTO for {@link ProductMaterialsRelation}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductMaterialsRelationForSchedulingDto implements Serializable {

    Long id;

    Long materialProductEntityId;

    Integer amount;

}
