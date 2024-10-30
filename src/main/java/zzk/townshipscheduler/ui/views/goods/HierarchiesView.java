package zzk.townshipscheduler.ui.views.goods;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import zzk.townshipscheduler.backend.service.GoodsService;
import zzk.townshipscheduler.port.GoodsHierarchy;

import java.util.Collection;

@Route
@Menu(order = 4d)
public class HierarchiesView
        extends VerticalLayout {

    private final GoodsService goodsService;

    private final ObjectMapper objectMapper;


    public HierarchiesView(GoodsService goodsService, ObjectMapper objectMapper) {
        this.goodsService = goodsService;
        this.objectMapper = objectMapper;

        add(new Button("Calc Goods Hierarchies", clicked -> {
            Collection<GoodsHierarchy> result = goodsService.calcGoodsHierarchies();
            String content = null;
            try {
                content = this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                content = result.toString();
            }
            add(new Paragraph(content));
        }));
    }

}
