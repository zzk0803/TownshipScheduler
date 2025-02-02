package zzk.townshipscheduler.backend.persistence.select;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.ProducingStructureType;

import java.io.Serializable;
import java.util.Set;

/**
 * DTO for {@link FieldFactoryInfoEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldFactoryInfoEntityDto implements Serializable {

    private static final long serialVersionUID = 6446932439841212593L;

     Long id;

     String category;

     Integer level;

     Set<ProductEntityDtoJustId> portfolioGoods;

     ProducingStructureType producingType;

     Integer defaultInstanceAmount;

     Integer defaultProducingCapacity;

     Integer defaultReapWindowCapacity;

     Integer maxProducingCapacity;

     Integer maxReapWindowCapacity;

     Integer maxInstanceAmount;

}
