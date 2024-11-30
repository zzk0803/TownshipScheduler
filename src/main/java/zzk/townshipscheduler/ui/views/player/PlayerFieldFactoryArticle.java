package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.service.PlayerService;
import zzk.townshipscheduler.ui.eventbus.UiEventBus;

import java.util.Map;

class PlayerFieldFactoryArticle extends Composite<VerticalLayout> {

    private final PlayerEntity currentPlayer;

    private final PlayerService playerService;

    private final Grid<FieldFactoryEntity> factoryEntityGrid;

    public PlayerFieldFactoryArticle(PlayerEntity player, PlayerService playerService) {
        this.currentPlayer = player;
        this.playerService = playerService;

        getContent().add(buildMenuBar());

        factoryEntityGrid = new Grid<>(FieldFactoryEntity.class, false);
        factoryEntityGrid.addColumn(
                fieldFactory -> fieldFactory.getFieldFactoryInfoEntity().getCategory()
        ).setHeader("Field&Factory Type");
        factoryEntityGrid.addColumn(
                fieldFactoryEntity -> (long) fieldFactoryEntity.getInstanceSequenceDetailsMap().keySet().size()
        ).setHeader("Instance").setFlexGrow(1);
        factoryEntityGrid.setItemDetailsRenderer(
                new ComponentRenderer<>(fieldFactoryEntity -> {
                    Map<Integer, FieldFactoryEntity.FieldFactoryDetails> instanceSequenceDetailsMap = fieldFactoryEntity.getInstanceSequenceDetailsMap();
                    VerticalLayout renderedContent = new VerticalLayout();
                    renderedContent.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
                    renderedContent.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
                    instanceSequenceDetailsMap.entrySet().stream()
                            .map(integerFieldFactoryDetailsEmbedEntry -> {
                                Integer sequence = integerFieldFactoryDetailsEmbedEntry.getKey();
                                FieldFactoryEntity.FieldFactoryDetails factoryDetailsEmbed = integerFieldFactoryDetailsEmbedEntry.getValue();
                                HorizontalLayout horizontalLayout = new HorizontalLayout();
                                horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
                                horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.START);
                                horizontalLayout.add(
                                        new Text("#" + sequence),
                                        new Text(" Producing Length=" + factoryDetailsEmbed.getProducingLength()),
                                        new Text(" Reap Window Size=" + factoryDetailsEmbed.getReapWindowSize())
                                );
                                return horizontalLayout;
                            })
                            .forEach(renderedContent::add);
                    return renderedContent;
                }
                )
        );
        getContent().addAndExpand(factoryEntityGrid);
    }

    public MenuBar buildMenuBar() {
        MenuBar fieldFactoryGridMenuBar = new MenuBar();
        fieldFactoryGridMenuBar.setWidthFull();
        fieldFactoryGridMenuBar.addThemeVariants(
                MenuBarVariant.LUMO_ICON, MenuBarVariant.LUMO_END_ALIGNED
        );

        MenuItem menuItem = fieldFactoryGridMenuBar.addItem(VaadinIcon.PLUS.create());
        menuItem.addSingleClickListener(menuItemClickEvent -> {
            Dialog dialog = new Dialog(new PlayerFieldFactoryArticleForm(currentPlayer, playerService));
            dialog.setSizeFull();
            dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);

            Dialog.DialogHeader header = dialog.getHeader();
            HorizontalLayout dialogHeaderWrapper = new HorizontalLayout();
            dialogHeaderWrapper.setWidthFull();
            Button closeBtn = new Button(
                    VaadinIcon.CLOSE.create(),
                    clicked -> {
                        dialog.close();
                    }
            );
            closeBtn.getStyle().set("margin-left", "auto");
            dialogHeaderWrapper.add(closeBtn);
            header.add(dialogHeaderWrapper);

            Dialog.DialogFooter footer = dialog.getFooter();
            footer.add(
                    new Button(
                            "Ok",
                            okClickEvent -> {
                                UiEventBus.publish(new PlayerFieldFactoryPersistRequestEvent(this, false));
                                dialog.close();
                                factoryEntityGrid.getDataProvider().refreshAll();
                            }
                    )
            );

            dialog.open();
        });
        return fieldFactoryGridMenuBar;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        factoryEntityGrid.setItems(playerService.findFieldFactoryEntityByPlayer(currentPlayer));
    }


}
