package zzk.townshipscheduler.backend.persistence.select;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.ProductManufactureInfoEntity;

import java.io.Serializable;

/**
 * DTO for {@link ProductManufactureInfoEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductManufactureInfoEntityDtoJustId implements Serializable {

    private static final long serialVersionUID = -6684562508245827316L;

    private final Long id;

}
