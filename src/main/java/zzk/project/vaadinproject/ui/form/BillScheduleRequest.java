package zzk.project.vaadinproject.ui.form;

import lombok.Data;
import zzk.project.vaadinproject.backend.persistence.Bill;

import java.util.List;

@Data
public class BillScheduleRequest {

    private List<Bill> bills;

}
