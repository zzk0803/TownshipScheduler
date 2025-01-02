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
public class AccountEntityDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1647801883638465225L;

     Long id;

     String username;

     byte[] profilePicture;

}
