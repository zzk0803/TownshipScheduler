package zzk.townshipscheduler.backend.persistence;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GoodsHierarchy {

    private Long goodId;

    private List<Long> composite;

    private Map<Long, Integer> materials;

}
