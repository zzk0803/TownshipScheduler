package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.ui.views.product.ProductView;

import java.util.Comparator;
import java.util.function.Function;

public class ProductsBriefGrid
        extends Grid<ProductEntity> {

    public ProductsBriefGrid(
            Function<ProductEntity, Image> productEntityImageFunction
    ) {
        this.addComponentColumn(
                        productEntity -> {
                            String productEntityName = productEntity.getName();
                            return new RouterLink(
                                    productEntityName,
                                    ProductView.class,
                                    new RouteParameters(
                                            new RouteParam("productName", productEntityName)
                                    )
                            );
                        }
                )
                .setHeader("Name")
                .setAutoWidth(true);
        this.addColumn(
                        new ComponentRenderer<>(productEntityImageFunction::apply)
                )
                .setHeader("Image")
                .setAutoWidth(true);
        this.addColumn(ProductEntity::getLevel)
                .setHeader("Required Level")
                .setSortable(true)
                .setComparator(Comparator.comparingInt(ProductEntity::getLevel))
                .setAutoWidth(true);
        this.addColumn(ProductEntity::getCategory)
                .setHeader("Category")
                .setAutoWidth(true);
        this.addColumn(ProductEntity::getDurationString)
                .setHeader("Producing Duration")
                .setAutoWidth(true);
        this.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        this.setSelectionMode(Grid.SelectionMode.NONE);
    }

}
