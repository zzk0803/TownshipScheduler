package zzk.townshipscheduler.ui.views.goods;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.Setter;
import zzk.townshipscheduler.ui.GoodsCategoriesPanel;

@Route
@Menu(order = 2d)
public class GoodsView
        extends VerticalLayout {

    public static final String ROUTE = "goods";

    @Getter
    @Setter
    private GoodsViewPresenter goodsViewPresenter;

    public GoodsView(
            GoodsViewPresenter goodsViewPresenter,
            GoodsCategoriesPanel goodsCategoriesPanel
    ) {
        this.goodsViewPresenter = goodsViewPresenter;
        goodsViewPresenter.setView(this);
        setupView();
        addAndExpand(goodsCategoriesPanel);
    }

    private void setupView() {
        setSizeFull();
        setSpacing(false);
        setMargin(false);
        setPadding(false);
    }

}
