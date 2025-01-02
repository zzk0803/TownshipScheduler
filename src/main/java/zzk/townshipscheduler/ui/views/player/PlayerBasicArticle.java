package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import zzk.townshipscheduler.backend.persistence.AccountEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.service.PlayerService;

import java.util.Optional;

class PlayerBasicArticle extends Composite<VerticalLayout> {

    private PlayerEntity player;

    private PlayerService playerService;

    private TextField nameField;

    private IntegerField levelField;

    private IntegerField fieldAmountField;

    private Binder<PlayerEntity> playerEntityBinder;

    public PlayerBasicArticle(PlayerEntity player, PlayerService playerService) {
        this.player = player;
        this.playerService = playerService;
        playerEntityBinder = new Binder<>(PlayerEntity.class);
        nameField = new TextField("Name");
        levelField = new IntegerField("Level");
        fieldAmountField = new IntegerField("Field Amount");
        playerEntityBinder.bindReadOnly(
                nameField,
                playerEntity -> Optional.ofNullable(playerEntity.getAccount())
                        .map(AccountEntity::getName)
                        .orElse("NULL")
        );
        playerEntityBinder.forField(levelField)
                .withValidator((integer, valueContext) -> integer > 0
                        ? ValidationResult.ok()
                        : ValidationResult.error("level number should >0")
                )
                .bind(PlayerEntity::getLevel, PlayerEntity::setLevel);
        playerEntityBinder.forField(fieldAmountField)
                .withValidator((integer, valueContext) -> integer > 0
                        ? ValidationResult.ok()
                        : ValidationResult.error("field number should >0"))
                .bind(PlayerEntity::getFieldAmount, PlayerEntity::setFieldAmount);
        playerEntityBinder.setBean(player);

        getContent().add(nameField, levelField, fieldAmountField);

        getContent().add(buildUpdatePlayerButton(player));
    }

    private Button buildUpdatePlayerButton(PlayerEntity player) {
        Button button = new Button("Update");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(clicked -> {
            PlayerEntity savedPlayer = playerService.emergeAndUpdate(player);

            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Extra Transaction");
            dialog.setText(
                    "shall we setup you ability to you corresponded level?"
            );

            dialog.setRejectable(true);
            dialog.setRejectText("Discard");
            dialog.addRejectListener(event -> dialog.close());

            dialog.setConfirmText("OK");
            dialog.addConfirmListener(event -> {
                playerService.playerFactoryToCorrespondedLevelInBatch(savedPlayer);
            });

            dialog.open();
        });
        return button;
    }


}
