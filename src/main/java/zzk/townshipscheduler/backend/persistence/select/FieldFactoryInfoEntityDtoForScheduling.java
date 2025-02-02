package zzk.townshipscheduler.backend.persistence.select;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * DTO for {@link FieldFactoryInfoEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldFactoryInfoEntityDtoForScheduling implements Serializable {

    @Serial
    private static final long serialVersionUID = -4399995313619593776L;

     Long id;

     Integer level;

     Set<ProductEntityDtoJustId> portfolioGoods;

     ProducingStructureType producingType;

}
