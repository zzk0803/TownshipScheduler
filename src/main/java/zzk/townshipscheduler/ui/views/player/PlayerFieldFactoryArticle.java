package zzk.townshipscheduler.ui.views.player;

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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.ui.utility.VaadinUiEventBus;

@Getter
class PlayerFieldFactoryArticle
        extends Composite<VerticalLayout> {

    private final PlayerViewPresenter playerViewPresenter;

    private final Grid<FieldFactoryEntity> factoryEntityGrid;

    public PlayerFieldFactoryArticle(PlayerViewPresenter playerViewPresenter) {
        this.playerViewPresenter = playerViewPresenter;

        getContent().add(buildMenuBar());
        getContent().addAndExpand(this.factoryEntityGrid = buildFieldFactoryGrid());
    }

    private @NonNull Grid<FieldFactoryEntity> buildFieldFactoryGrid() {
        final Grid<FieldFactoryEntity> factoryEntityGrid;
        factoryEntityGrid = new Grid<>(FieldFactoryEntity.class, false);
        Grid.Column<FieldFactoryEntity> typeColumn
                = factoryEntityGrid.addColumn(
                fieldFactory -> fieldFactory.getFieldFactoryInfoEntity().getCategory()
        ).setHeader("Field&FactoryType");
        Grid.Column<FieldFactoryEntity> producingLengthColumn
                = factoryEntityGrid.addColumn(FieldFactoryEntity::getProducingLength)
                .setHeader("Factory Producing Length");
        Grid.Column<FieldFactoryEntity> factoryReapWindowSizeColumn
                = factoryEntityGrid.addColumn(FieldFactoryEntity::getReapWindowSize)
                .setHeader("Factory Reap Window Size");
        factoryEntityGrid.setItems(playerViewPresenter.findFieldFactoryEntityByPlayer());
        return factoryEntityGrid;
    }

    public MenuBar buildMenuBar() {
        MenuBar fieldFactoryGridMenuBar = new MenuBar();
        fieldFactoryGridMenuBar.setWidthFull();
        fieldFactoryGridMenuBar.addThemeVariants(
                MenuBarVariant.LUMO_ICON, MenuBarVariant.LUMO_END_ALIGNED
        );

        MenuItem menuItem = fieldFactoryGridMenuBar.addItem(VaadinIcon.PLUS.create());
        menuItem.addSingleClickListener(menuItemClickEvent -> {
            PlayerFieldFactoryArticleForm playerFieldFactoryArticleForm = new PlayerFieldFactoryArticleForm(this.playerViewPresenter);
            Dialog dialog = new Dialog(playerFieldFactoryArticleForm);
            dialog.setSizeUndefined();
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
            dialogHeaderWrapper.add(new Text("New Factory Instance"));
            dialogHeaderWrapper.add(closeBtn);
            header.add(dialogHeaderWrapper);

            Dialog.DialogFooter footer = dialog.getFooter();
            footer.add(
                    new Button(
                            "Ok",
                            okClickEvent -> {
                                playerFieldFactoryArticleForm.submit();
                                dialog.close();
                                factoryEntityGrid.getDataProvider().refreshAll();
                            }
                    )
            );

            dialog.open();
        });
        return fieldFactoryGridMenuBar;
    }


}
