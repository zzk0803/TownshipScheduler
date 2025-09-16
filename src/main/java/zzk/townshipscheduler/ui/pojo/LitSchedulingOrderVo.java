package zzk.townshipscheduler.ui.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.io.Serializable;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class LitSchedulingOrderVo implements Serializable {

    private long id;

    private String orderType;

    private String deadline;

}
