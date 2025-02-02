package zzk.townshipscheduler.backend.persistence.select;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO for {@link FieldFactoryEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldFactoryEntityDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 6591495423752740958L;

    Long id;

    FieldFactoryInfoEntityDtoJustId fieldFactoryInfoEntityDto;

    int producingLength;

    int reapWindowSize;

}
