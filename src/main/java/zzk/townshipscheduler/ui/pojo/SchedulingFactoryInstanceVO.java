package zzk.townshipscheduler.ui.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link zzk.townshipscheduler.backend.scheduling.model.BaseSchedulingFactoryInstance}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchedulingFactoryInstanceVO implements Serializable {

    private static final long serialVersionUID = 8503586802835134749L;

    Integer id;

    String categoryName;

    int seqNum;

    int producingLength;

    int reapWindowSize;


}
