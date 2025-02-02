package zzk.townshipscheduler.backend.persistence.select;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.AccountEntity;

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
