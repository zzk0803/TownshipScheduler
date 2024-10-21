package zzk.townshipscheduler.ui.views.goods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class GoodsViewPresenter {

    private static final Logger log = LoggerFactory.getLogger(GoodsViewPresenter.class);

    private GoodsView goodsView;

    public void setView(GoodsView goodsView) {
        this.goodsView = goodsView;
    }

}
