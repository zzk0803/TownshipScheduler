package zzk.townshipscheduler.backend.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * DTO for {@link WarehouseEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class WarehouseEntityDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 6382908534176363968L;

     Long id;

     Map<ProductEntityDtoJustId, Integer> itemAmountMap;

}
