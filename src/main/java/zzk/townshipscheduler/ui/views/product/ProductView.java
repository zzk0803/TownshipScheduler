package zzk.townshipscheduler.ui.views.product;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import zzk.townshipscheduler.ui.components.GoodsCategoriesPanel;

@Route("/product")
@Menu
@AnonymousAllowed
public class ProductView extends VerticalLayout {

    private final GoodsCategoriesPanel goodsCategoriesPanel;

    public ProductView(GoodsCategoriesPanel goodsCategoriesPanel) {
        this.goodsCategoriesPanel = goodsCategoriesPanel;
        setSizeFull();
        setPadding(false);
        addAndExpand(this.goodsCategoriesPanel);
    }

}
