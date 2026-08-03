package zzk.townshipscheduler.ui.pojo.scheduling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import zzk.townshipscheduler.backend.scheduling.model.SchedulingFactoryInstance;

import java.io.Serializable;

/**
 * DTO for {@link SchedulingFactoryInstance}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LitSchedulingFactoryInstanceVO(
        Integer id,
        String categoryName,
        int seqNum,
        int producingLength,
        int reapWindowSize,
        String factoryReadableIdentifier
) implements Serializable {

    private static final long serialVersionUID = 8503586802835134749L;

}
