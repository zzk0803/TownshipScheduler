package zzk.townshipscheduler.backend.crawling;


import zzk.townshipscheduler.backend.persistence.Goods;

import java.util.List;

public record TransferResult(List<Goods> goodsArrayList) {

}
