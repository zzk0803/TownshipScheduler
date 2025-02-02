package zzk.townshipscheduler.backend.persistence.select;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.ProductMaterialsRelation;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO for {@link ProductMaterialsRelation}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductMaterialsRelationDtoForScheduling implements Serializable {

    @Serial
    private static final long serialVersionUID = -5248954614529120478L;

     ProductEntityDtoJustId material;

     ProductManufactureInfoEntityDtoJustId productManufactureInfo;

     Integer amount;

}
