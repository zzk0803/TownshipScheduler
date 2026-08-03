package zzk.townshipscheduler.ui.pojo.scheduling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LitSchedulingOrderVo(
        long id,
        String orderType,
        String deadline
) implements Serializable {

}
