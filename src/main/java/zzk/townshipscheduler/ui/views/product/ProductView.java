package zzk.townshipscheduler.ui.views.product;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.Getter;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.ui.components.ProductDetailPanel;
import zzk.townshipscheduler.ui.components.ProductsCategoriesPanel;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Getter
@Route("/products/:productName?")
@Menu(
        title = "Products",
        order = 3.00d
)
@AnonymousAllowed
public class ProductView
        extends VerticalLayout
        implements BeforeEnterObserver {

    private final ProductViewPresenter productViewPresenter;

    private final ProductsCategoriesPanel productsCategoriesPanel;

    public ProductView(ProductViewPresenter productViewPresenter) {
        this.productViewPresenter = productViewPresenter;
        this.productViewPresenter.setProductView(this);
        this.productsCategoriesPanel = new ProductsCategoriesPanel(this.productViewPresenter);

        productViewStyle();
    }

    private void productViewStyle() {
        addClassName("product-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setMargin(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        removeAll();
        beforeEnterEvent.getRouteParameters()
                .get("productName")
                .ifPresentOrElse(
                        productName -> productDetailUi(URLDecoder.decode(productName, StandardCharsets.UTF_8)),
                        this::productCategoriesUi
                );
    }

    private void productDetailUi(String productName) {
        ProductEntity productEntity = this.productViewPresenter.queryProduct(productName);
        addAndExpand(new ProductDetailPanel(productEntity,this.productViewPresenter));
        setFlexShrink(1.0, this.productsCategoriesPanel);
    }

    private void productCategoriesUi() {
        addAndExpand(this.productsCategoriesPanel);
        setFlexShrink(1.0, this.productsCategoriesPanel);
    }

}
