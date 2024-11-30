package zzk.townshipscheduler.pojo.form;

import lombok.Builder;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.OrderEntity;

import java.util.List;

@Value
@Builder
public class BillScheduleRequest {

    List<OrderEntity> orderEntities;

}
