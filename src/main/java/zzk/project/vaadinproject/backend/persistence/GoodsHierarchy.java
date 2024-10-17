package zzk.project.vaadinproject.backend.persistence;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GoodsHierarchy {

    private Long goodId;

    private List<Long> advancedProductList;

    private Map<Long, Integer> consistMaterialMap;

}
