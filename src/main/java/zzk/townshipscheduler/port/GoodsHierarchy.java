package zzk.townshipscheduler.port;

import lombok.Builder;
import lombok.Data;
import zzk.townshipscheduler.backend.persistence.Goods;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GoodsHierarchy {

    private GoodId goodId;

    private List<GoodId> composite;

    private Map<GoodId, Integer> materials;

    public boolean boolAtomicGood() {
        return materials == null || materials.isEmpty();
    }

}
