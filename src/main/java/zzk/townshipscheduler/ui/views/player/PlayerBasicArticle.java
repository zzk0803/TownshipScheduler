package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import lombok.Getter;
import zzk.townshipscheduler.backend.persistence.AccountEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;

import java.util.Optional;

class PlayerBasicArticle
        extends Composite<VerticalLayout> {

    private final PlayerViewPresenter playerViewPresenter;

    private PlayerForm playerForm;

    public PlayerBasicArticle(PlayerViewPresenter playerViewPresenter) {
        this.playerViewPresenter = playerViewPresenter;
        if (!this.playerViewPresenter.validate()) {
            throw new IllegalStateException("player not exist");
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getContent().removeAll();
        this.playerForm = new PlayerForm(this.playerViewPresenter.getPlayer());

        getContent().add(playerForm);
        getContent().add(
                new VerticalLayout(
                        new Button("Use Template (level=30,fieldAmount=30,warehouseSize=300)") {{
                            addThemeVariants(ButtonVariant.TERTIARY);
                            addClickListener(click -> {
                                PlayerForm theForm = PlayerBasicArticle.this.playerForm;
                                theForm.getLevelField()
                                        .setValue(30);
                                theForm.getFieldAmountField()
                                        .setValue(30);
                                theForm.getWarehouseSizeField()
                                        .setValue(300);
                            });
                        }}, new Button("Use Template (level=60,fieldAmount=100,warehouseSize=600)") {{
                    addThemeVariants(ButtonVariant.TERTIARY);
                    addClickListener(click -> {
                        PlayerForm theForm = PlayerBasicArticle.this.playerForm;
                        theForm.getLevelField()
                                .setValue(60);
                        theForm.getFieldAmountField()
                                .setValue(100);
                        theForm.getWarehouseSizeField()
                                .setValue(600);
                    });
                }}, new Button("Use Template (level=90,fieldAmount=120,warehouseSize=900)") {{
                    addThemeVariants(ButtonVariant.TERTIARY);
                    addClickListener(click -> {
                        PlayerForm theForm = PlayerBasicArticle.this.playerForm;
                        theForm.getLevelField()
                                .setValue(90);
                        theForm.getFieldAmountField()
                                .setValue(120);
                        theForm.getWarehouseSizeField()
                                .setValue(900);
                    });
                }}
                ));
        getContent().add(buildUpdatePlayerButton());
    }

    private Button buildUpdatePlayerButton() {
        Button button = new Button("Update");
        button.setDisableOnClick(true);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(clicked -> {
            PlayerEntity playerEntity = this.playerForm.submitAndGet();
            if (playerEntity != null) {
                playerViewPresenter.emergeAndUpdate(playerEntity);
                ConfirmDialog confirmDialog = new ConfirmDialog(
                        "Update Player Info", "Done", "OK", confirmClicked -> {
                    playerForm.reflash();
                    button.setEnabled(true);
                }
                );
                confirmDialog.open();
            } else {
                Notification.show("not success");
            }
        });
        return button;
    }

    @Getter
    private class PlayerForm
            extends FormLayout {

        private final IntegerField levelField;

        private final IntegerField fieldAmountField;

        private final IntegerField warehouseSizeField;

        PlayerEntity player = new PlayerEntity();

        Binder<PlayerEntity> binder;

        public PlayerForm(PlayerEntity player) {
            this.binder = new Binder<>();
            this.binder.setBean(player);

            this.setResponsiveSteps(
                    new ResponsiveStep("0", 1)
            );

            var nameField = new TextField("Name");
            levelField = new IntegerField("Level");
            fieldAmountField = new IntegerField("Field Amount");
            warehouseSizeField = new IntegerField("Warehouse Size");

            binder.bindReadOnly(
                    nameField,
                    playerEntity -> Optional.ofNullable(playerEntity.getAccount())
                            .map(AccountEntity::getName)
                            .orElse("NULL")
            );
            binder.forField(levelField)
                    .withValidator((integer, valueContext) -> integer > 0
                            ? ValidationResult.ok()
                            : ValidationResult.error("level number should >0")
                    )
                    .bind(PlayerEntity::getLevel, PlayerEntity::setLevel);
            binder.forField(fieldAmountField)
                    .withValidator((integer, valueContext) -> integer > 0
                            ? ValidationResult.ok()
                            : ValidationResult.error("field number should >0"))
                    .bind(PlayerEntity::getFieldAmount, PlayerEntity::setFieldAmount);
            binder.forField(warehouseSizeField)
                    .withValidator((integer, valueContext) -> integer > 0
                            ? ValidationResult.ok()
                            : ValidationResult.error("warehouse size should >0"))
                    .bind(PlayerEntity::getWarehouseSize, PlayerEntity::setWarehouseSize);

            add(nameField);
            add(levelField);
            add(fieldAmountField);
            add(warehouseSizeField);
        }

        public PlayerEntity submitAndGet() {
            return this.binder.getBean();
        }

        public void reflash() {
            player = playerViewPresenter.getPlayer();
            binder.readBean(player);
        }

    }

}
