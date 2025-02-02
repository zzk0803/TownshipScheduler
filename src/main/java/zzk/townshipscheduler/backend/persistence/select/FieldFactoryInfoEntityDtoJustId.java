package zzk.townshipscheduler.backend.persistence.select;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;

import java.io.Serializable;

/**
 * DTO for {@link FieldFactoryInfoEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldFactoryInfoEntityDtoJustId implements Serializable {

    private static final long serialVersionUID = -1011625580293970190L;

    private final Long id;

}
