package zzk.townshipscheduler.backend.persistence.select;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO for {@link ProductEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEntityDtoJustId implements Serializable {

    @Serial
    private static final long serialVersionUID = -5447911415406742188L;

     Long id;

}
