package zzk.project.vaadinproject.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import zzk.project.vaadinproject.backend.persistence.GoodsHierarchy;
import zzk.project.vaadinproject.backend.service.GoodsService;

import java.util.Collection;

@Route
@Menu(order = 4d)
public class HierarchiesView
        extends VerticalLayout {

    private final GoodsService goodsService;

    public HierarchiesView(GoodsService goodsService) {
        this.goodsService = goodsService;

        add(new Button("Calc Goods Hierarchies", clicked -> {
            Collection<GoodsHierarchy> result = goodsService.calcGoodsHierarchies();
            add(new Paragraph(result.toString()));
        }));
    }

}
