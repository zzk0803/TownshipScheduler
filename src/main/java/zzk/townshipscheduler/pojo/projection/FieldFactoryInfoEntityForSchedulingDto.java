package zzk.townshipscheduler.pojo.projection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.io.Serializable;
import java.util.Set;

/**
 * DTO for {@link zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldFactoryInfoEntityForSchedulingDto implements Serializable {

    Long id;

    String category;

    Integer level;

    Set<Long> portfolioGoodProductEntityIds;

    boolean slotWorkingParallel;

    Integer maxInstanceCapacity;

    Integer defaultQueueCapacity;

    Integer maxQueueCapacity;

    Integer defaultWindowCapacity;

    Integer maxWindowCapacity;

}
