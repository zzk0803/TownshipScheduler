package zzk.townshipscheduler.port.form;

import lombok.Builder;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.Order;

import java.util.List;

@Value
@Builder
public class BillScheduleRequest {

    List<Order> orders;

}
