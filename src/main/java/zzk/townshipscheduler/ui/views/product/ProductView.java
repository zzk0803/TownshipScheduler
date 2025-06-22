package zzk.townshipscheduler.ui.views.product;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import zzk.townshipscheduler.ui.components.ProductCategoriesPanel;

@Route("/products")
@Menu(title = "Products", order = 3.00d)
@AnonymousAllowed
public class ProductView extends VerticalLayout {

    private final ProductCategoriesPanel productCategoriesPanel;

    public ProductView(ProductCategoriesPanel productCategoriesPanel) {
        this.productCategoriesPanel = productCategoriesPanel;
        setSizeFull();
        setPadding(false);
        addAndExpand(this.productCategoriesPanel);
    }

}
