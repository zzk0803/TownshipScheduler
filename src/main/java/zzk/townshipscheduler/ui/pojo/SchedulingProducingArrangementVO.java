package zzk.townshipscheduler.ui.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class SchedulingProducingArrangementVO {

    int id;

    String uuid;

    String product;

    String arrangeFactory;

    String arrangeFactoryId;

    String producingDuration;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime arrangeDateTime;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime gameProducingDateTime;

    @JsonInclude
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime gameCompletedDateTime;

}
