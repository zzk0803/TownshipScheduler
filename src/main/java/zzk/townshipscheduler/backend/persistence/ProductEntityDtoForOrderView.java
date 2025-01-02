package zzk.townshipscheduler.backend.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO for {@link ProductEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEntityDtoForOrderView implements Serializable {

    @Serial
    private static final long serialVersionUID = 3168846299606883374L;

     String name;

     byte[] crawledAsImageImageBytes;

}
