package zzk.townshipscheduler.adopting.form;

import lombok.Builder;
import lombok.Value;
import zzk.townshipscheduler.backend.persistence.Bill;

import java.util.List;

@Value
@Builder
public class BillScheduleRequest {

    List<Bill> bills;

}
