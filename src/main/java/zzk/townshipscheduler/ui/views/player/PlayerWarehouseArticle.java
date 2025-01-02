package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.StreamResource;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.persistence.ProductEntity;
import zzk.townshipscheduler.backend.persistence.WarehouseEntity;
import zzk.townshipscheduler.backend.service.PlayerService;
import zzk.townshipscheduler.ui.components.GoodsCategoriesPanel;
import zzk.townshipscheduler.ui.eventbus.UiEventBus;

import java.io.ByteArrayInputStream;
import java.util.Map;

class PlayerWarehouseArticle extends Composite<VerticalLayout> {

    private final PlayerEntity currentPlayer;

    private final PlayerService playerService;

    private final Grid<Map.Entry<ProductEntity, Integer>> grid;

    public PlayerWarehouseArticle(
            PlayerEntity playerEntity,
            PlayerService playerService,
            GoodsCategoriesPanel goodsCategoriesPanel
    ) {
        this.currentPlayer = playerEntity;
        this.playerService = playerService;

        setupMenuBar(goodsCategoriesPanel);
        grid = new Grid<>();
        grid.addColumn(new ComponentRenderer<>(productEntityIntegerEntry -> {
            HorizontalLayout result = new HorizontalLayout();
            result.add(
                    new VerticalLayout(
                            new Image(
                                    new StreamResource(
                                            "productEntityIntegerEntry.getKey().getName()",
                                            () -> new ByteArrayInputStream(productEntityIntegerEntry.getKey()
                                                    .getCrawledAsImage()
                                                    .getImageBytes())
                                    ), productEntityIntegerEntry.getKey().getName()),
                            new Text(productEntityIntegerEntry.getKey().getName())
                    ),
                    new Text(" X" + productEntityIntegerEntry.getValue())
            );
            return result;
        })).setHeader("item-amount");

        UiEventBus.subscribe(
                this,
                PlayerWarehouseArticleGridUpdateEvent.class,
                componentEvent -> {
                    grid.getDataProvider().refreshAll();
                }
        );
        getContent().addAndExpand(grid);
    }

    public void setupMenuBar(GoodsCategoriesPanel goodsCategoriesPanel) {
        MenuBar menuBar = new MenuBar();
        menuBar.setWidthFull();
        menuBar.addThemeVariants(
                MenuBarVariant.LUMO_ICON,
                MenuBarVariant.LUMO_END_ALIGNED
        );

        MenuItem menuItem = menuBar.addItem(VaadinIcon.PLUS.create());
        menuItem.addSingleClickListener(menuItemClickEvent -> {
            Dialog dialog = new Dialog(goodsCategoriesPanel);
            dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);
            dialog.setSizeFull();

            Dialog.DialogFooter footer = dialog.getFooter();
            HorizontalLayout dialogFooterWrapper = new HorizontalLayout();
            dialogFooterWrapper.setWidthFull();
            dialogFooterWrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            dialogFooterWrapper.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            footer.addComponentAsFirst(dialogFooterWrapper);

            IntegerField footerAmountField = new IntegerField();
            footerAmountField.setPlaceholder("Amount");
            dialogFooterWrapper.add(footerAmountField);
            dialogFooterWrapper.add(
                    new Button(
                            "Ok",
                            okClickEvent -> {
                                goodsCategoriesPanel.consumeSelected(productEntity -> {
                                    WarehouseEntity updatedWarehouse = playerService.updateWarehouseStock(
                                            playerService.findWarehouseEntityByPlayerEntity(currentPlayer),
                                            productEntity,
                                            footerAmountField.getOptionalValue().orElse(1)
                                    );
                                });
                                UiEventBus.publish(new PlayerWarehouseArticleGridUpdateEvent());
                                dialog.close();
                            }
                    ));
            dialog.open();
        });
        getContent().add(menuBar);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        grid.setItems(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    WarehouseEntity warehouseEntity = playerService.findWarehouseEntityByPlayerEntity(currentPlayer);

                    Map<ProductEntity, Integer> itemAmountMap = warehouseEntity.getProductAmountMap();
                    return itemAmountMap.entrySet().stream().skip(offset).limit(limit);
                }
        );
    }

    class PlayerWarehouseArticleGridUpdateEvent extends ComponentEvent<PlayerWarehouseArticle> {

        public PlayerWarehouseArticleGridUpdateEvent() {
            this(PlayerWarehouseArticle.this, false);
        }

        public PlayerWarehouseArticleGridUpdateEvent(PlayerWarehouseArticle source, boolean fromClient) {
            super(source, fromClient);
        }

    }

}
