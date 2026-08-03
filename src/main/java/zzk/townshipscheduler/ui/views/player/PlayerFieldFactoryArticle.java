package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
class PlayerFieldFactoryArticle
        extends Composite<VerticalLayout> {

    private final PlayerViewPresenter playerViewPresenter;

    private final List<FieldFactoryEntity> fieldFactoryEntityForPlayer = new ArrayList<>();

    private final ListDataProvider<FieldFactoryEntity> fieldFactoryEntityListDataProvider = new ListDataProvider<>(fieldFactoryEntityForPlayer);

    private Grid<FieldFactoryEntity> fieldFactoryEntityGrid;

    public PlayerFieldFactoryArticle(PlayerViewPresenter playerViewPresenter) {
        this.playerViewPresenter = playerViewPresenter;
        if (!this.playerViewPresenter.validate()) {
            throw new IllegalStateException("player not exist");
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getContent().removeAll();

        loadPlayerFieldFactory();

        getContent().add(buildMenuBar());
        getContent().addAndExpand(this.fieldFactoryEntityGrid = buildFieldFactoryGrid());
    }

    private void loadPlayerFieldFactory() {
        List<FieldFactoryEntity> factoryEntities = this.playerViewPresenter.findFieldFactoryEntityByPlayer();
        this.fieldFactoryEntityForPlayer.clear();
        this.fieldFactoryEntityForPlayer.addAll(factoryEntities);
    }

    private @NonNull Grid<FieldFactoryEntity> buildFieldFactoryGrid() {
        final Grid<FieldFactoryEntity> factoryEntityGrid;
        factoryEntityGrid = new Grid<>(FieldFactoryEntity.class, false);
        factoryEntityGrid.addColumn(
                        fieldFactory -> fieldFactory.getFieldFactoryInfoEntity()
                                .getCategory()
                )
                .setHeader("Field&FactoryType");
        factoryEntityGrid.addColumn(FieldFactoryEntity::getReapWindowSize)
                .setHeader("Factory Reap Window Size");
        factoryEntityGrid.setItems(fieldFactoryEntityForPlayer);
        factoryEntityGrid.addItemDoubleClickListener(event -> {
            PlayerFieldFactoryArticleForm playerFieldFactoryArticleForm = new PlayerFieldFactoryArticleForm(this.playerViewPresenter, event.getItem());
            Dialog dialog = new Dialog(playerFieldFactoryArticleForm);
            dialog.setWidth(67.8F, Unit.VW);
            Dialog.DialogHeader header = dialog.getHeader();
            HorizontalLayout dialogHeaderWrapper = new HorizontalLayout();
            dialogHeaderWrapper.setWidthFull();
            Button closeBtn = new Button(
                    VaadinIcon.CLOSE.create(),
                    clicked -> {
                        dialog.close();
                    }
            );
            closeBtn.getStyle()
                    .set("margin-left", "auto");
            dialogHeaderWrapper.add(new Text("Edit Factory Instance"));
            dialogHeaderWrapper.add(closeBtn);
            header.add(dialogHeaderWrapper);

            Dialog.DialogFooter footer = dialog.getFooter();
            footer.add(
                    new Button(
                            "Ok",
                            okClickEvent -> {
                                boolean submitted = playerFieldFactoryArticleForm.submit(true);
                                if (submitted) {
                                    dialog.close();
                                    UI.getCurrentOrThrow()
                                            .access(this::reloadPlayerFieldFactory);
                                }
                            }
                    )
            );

            dialog.open();
        });
        return factoryEntityGrid;
    }

    public MenuBar buildMenuBar() {
        MenuBar fieldFactoryGridMenuBar = new MenuBar();
        fieldFactoryGridMenuBar.setWidthFull();
        fieldFactoryGridMenuBar.addThemeVariants(
                MenuBarVariant.LUMO_ICON, MenuBarVariant.LUMO_END_ALIGNED
        );

        MenuItem menuItem = fieldFactoryGridMenuBar.addItem(VaadinIcon.ERASER.create());
        menuItem.setDisableOnClick(true);
        menuItem.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog(
                    "Clear All FieldFactory", "Remove All FieldFactory Instance!This is Not Recoverable", "Execute", confirmClick -> {
                PlayerEntity player = playerViewPresenter.getPlayer();
                playerViewPresenter.clearPlayerFieldFactory(player);
                menuItem.setEnabled(true);
                UI.getCurrentOrThrow()
                        .access(this::reloadPlayerFieldFactory);
            }
            );
            confirmDialog.open();
        });
        MenuItem newFieldFactoryDialogMenuItem = fieldFactoryGridMenuBar.addItem(VaadinIcon.PLUS.create());
        newFieldFactoryDialogMenuItem.addSingleClickListener(newFieldFactoryDialog());
        MenuItem toPlayerLevelPropertiesMenuItem = fieldFactoryGridMenuBar.addItem("One Key To My Level Properties");
        toPlayerLevelPropertiesMenuItem.setDisableOnClick(true);
        toPlayerLevelPropertiesMenuItem.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog(
                    "ToCorrespondedLevelInBatch", "Setup All Available Field Factory Match Your Player Level", "Execute", confirmClick -> {
                playerViewPresenter.playerFactoryToCorrespondedLevelInBatch();
                toPlayerLevelPropertiesMenuItem.setEnabled(true);
                UI.getCurrentOrThrow()
                        .access(this::reloadPlayerFieldFactory);
            }
            );
            confirmDialog.open();
        });
        return fieldFactoryGridMenuBar;
    }

    private void reloadPlayerFieldFactory() {
        loadPlayerFieldFactory();
        getFieldFactoryEntityListDataProvider().refreshAll();
        getFieldFactoryEntityGrid().getDataProvider()
                .refreshAll();
    }

    private @NonNull ComponentEventListener<ClickEvent<MenuItem>> newFieldFactoryDialog() {
        return menuItemClickEvent -> {
            PlayerFieldFactoryArticleForm playerFieldFactoryArticleForm = new PlayerFieldFactoryArticleForm(this.playerViewPresenter);
            Dialog dialog = new Dialog(playerFieldFactoryArticleForm);
            dialog.setWidth(67.8F, Unit.VW);
            Dialog.DialogHeader header = dialog.getHeader();
            HorizontalLayout dialogHeaderWrapper = new HorizontalLayout();
            dialogHeaderWrapper.setWidthFull();
            Button closeBtn = new Button(
                    VaadinIcon.CLOSE.create(),
                    clicked -> {
                        dialog.close();
                    }
            );
            closeBtn.getStyle()
                    .set("margin-left", "auto");
            dialogHeaderWrapper.add(new Text("New Factory Instance"));
            dialogHeaderWrapper.add(closeBtn);
            header.add(dialogHeaderWrapper);

            Dialog.DialogFooter footer = dialog.getFooter();
            footer.add(
                    new Button(
                            "Ok",
                            okClickEvent -> {
                                boolean submitted = playerFieldFactoryArticleForm.submit(false);
                                if (submitted) {
                                    dialog.close();
                                    UI.getCurrentOrThrow()
                                            .access(this::reloadPlayerFieldFactory);
                                }
                            }
                    )
            );

            dialog.open();
        };
    }

}
