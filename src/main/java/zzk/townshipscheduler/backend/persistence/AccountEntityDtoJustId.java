package zzk.townshipscheduler.backend.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO for {@link AccountEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountEntityDtoJustId implements Serializable {

    @Serial
    private static final long serialVersionUID = -7865645687051969972L;

    Long id;

}
