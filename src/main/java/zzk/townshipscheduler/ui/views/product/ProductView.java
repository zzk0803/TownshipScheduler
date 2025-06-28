package zzk.townshipscheduler.ui.views.product;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import zzk.townshipscheduler.ui.components.ProductsCategoriesPanel;

@Route("/products")
@Menu(title = "Products", order = 3.00d)
@AnonymousAllowed
public class ProductView extends VerticalLayout {

    private final ProductViewPresenter productViewPresenter;

    private ProductsCategoriesPanel productsCategoriesPanel;

    public ProductView(ProductViewPresenter productViewPresenter) {
        this.productViewPresenter = productViewPresenter;
        this.productViewPresenter.setProductView(this);

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setMargin(false);

        this.productsCategoriesPanel = new ProductsCategoriesPanel(this.productViewPresenter.fetchProducts());
        addAndExpand(this.productsCategoriesPanel);
        setFlexShrink(1.0, this.productsCategoriesPanel);
    }

}
