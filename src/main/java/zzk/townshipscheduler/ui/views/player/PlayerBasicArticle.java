package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import zzk.townshipscheduler.backend.persistence.AccountEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;

import java.util.Optional;

class PlayerBasicArticle extends Composite<VerticalLayout> {

    private final PlayerViewPresenter playerViewPresenter;

    private final PlayerForm playerForm;

    public PlayerBasicArticle(PlayerViewPresenter playerViewPresenter) {
        this.playerViewPresenter = playerViewPresenter;

        this.playerForm = new PlayerForm(this.playerViewPresenter.getPlayer());

        getContent().add(playerForm);

        getContent().add(buildUpdatePlayerButton());
    }

    private Button buildUpdatePlayerButton() {
        Button button = new Button("Update");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(clicked -> {
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
                playerViewPresenter.playerFactoryToCorrespondedLevelInBatch(this.playerForm.player);
                playerForm.reflash();
            });

            dialog.open();
        });
        return button;
    }

    private class PlayerForm extends FormLayout {

        PlayerEntity player;

        Binder<PlayerEntity> playerEntityBinder = new Binder<>();

        public PlayerForm(PlayerEntity player) {
            this.player = player;
            this.playerEntityBinder = new Binder<>(PlayerEntity.class);
            this.playerEntityBinder.setBean(this.player);

            this.setResponsiveSteps(
                    new ResponsiveStep("0", 1)
            );

            var nameField = new TextField("Name");
            var levelField = new IntegerField("Level");
            var fieldAmountField = new IntegerField("Field Amount");

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


            add(nameField);
            add(levelField);
            add(fieldAmountField);
        }

        public void reflash() {
            player = playerViewPresenter.getPlayer();
            playerEntityBinder.readBean(player);
        }

    }

}
