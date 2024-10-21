package zzk.townshipscheduler.backend.persistence;

import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link Goods}
 */
@Value
public class GoodsDtoForCalcHierarchies
        implements Serializable {

    private static final long serialVersionUID = -9217138897101674182L;

    private final Long id;

    private final String name;

    private final String category;

    private final String bomString;

    private final String durationString;

}
