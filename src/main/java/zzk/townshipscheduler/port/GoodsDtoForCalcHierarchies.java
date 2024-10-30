package zzk.townshipscheduler.port;

import lombok.Value;
import zzk.townshipscheduler.backend.persistence.Goods;

/**
 * DTO for {@link Goods}
 */
@Value
public class GoodsDtoForCalcHierarchies {

    Long id;

    String name;

    String category;

    String bomString;

    String durationString;

}
