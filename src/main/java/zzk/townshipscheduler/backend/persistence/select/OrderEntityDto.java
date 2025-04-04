package zzk.townshipscheduler.backend.persistence.select;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.OrderEntity;
import zzk.townshipscheduler.backend.OrderEntityScheduleState;
import zzk.townshipscheduler.backend.OrderType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for {@link OrderEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEntityDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 8047988690974064527L;

     Long id;

     OrderType orderType;

     LocalDateTime createdDateTime;

     boolean boolDeadLine;

     LocalDateTime deadLine;

     OrderEntityScheduleState billScheduleState;

     Map<ProductEntityDtoJustId, Integer> productAmountPairs;

     boolean boolFinished;

     LocalDateTime finishedDateTime;

}
