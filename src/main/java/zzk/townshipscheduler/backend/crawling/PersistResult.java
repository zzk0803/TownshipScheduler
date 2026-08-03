package zzk.townshipscheduler.backend.crawling;

import zzk.townshipscheduler.backend.persistence.ProductEntity;

import java.util.List;

public record PersistResult(List<ProductEntity> productEntityArrayList) {

}
