package zzk.townshipscheduler.ui.pojo;

import lombok.Value;

@Value
public class ProducingArrangementVO {

    private String id;

    private String uuid;

    private String product;

    private String arrangeFactory;

    private String arrangeDateTime;

    private String gameProducingDateTime;

    private String gameCompletedDateTime;

}
