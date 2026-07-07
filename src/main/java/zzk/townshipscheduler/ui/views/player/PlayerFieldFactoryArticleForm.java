package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.signals.local.ValueSignal;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Slf4j
class PlayerFieldFactoryArticleForm
        extends Composite<VerticalLayout> {

    private final Binder<FieldFactoryEntity> binder = new Binder<>();

    private final PlayerViewPresenter playerViewPresenter;

    private final ValueSignal<Boolean> availableFieldFactoryBooleanValueSignal = new ValueSignal<>(false);

    private List<FieldFactoryEntity> fieldFactoryEntityForPlayer;

    private List<FieldFactoryInfoEntity> availableFieldFactoryInfoForPlayer;

    private List<FieldFactoryInfoEntity> allFieldFactoryInfo;

    private ComboBox<FieldFactoryInfoEntity> fieldFactoryInfoEntityComboBox;

    private IntegerField producingLengthIntegerField;

    private IntegerField reapWindowSizeIntegerField;

    private ValueSignal<Boolean> createModeValueSignal = new ValueSignal<>(true);

    private ValueSignal<FieldFactoryInfoEntity> fieldFactoryInfoEntityValueSignal = new ValueSignal<>(FieldFactoryInfoEntity.NULL_EMPTY_VALUE);

    private ValueSignal<Integer> producingLengthSignal;

    private ValueSignal<Integer> reapWindowSignal;

    private FieldFactoryEntity fieldFactoryEntity;

    public PlayerFieldFactoryArticleForm(PlayerViewPresenter playerViewPresenter) {
        this.playerViewPresenter = playerViewPresenter;
        if (!this.playerViewPresenter.validate()) {
            throw new IllegalStateException("no player exist");
        }
    }

    public PlayerFieldFactoryArticleForm(PlayerViewPresenter playerViewPresenter, FieldFactoryEntity fieldFactoryEntity) {
        this.playerViewPresenter = playerViewPresenter;
        this.fieldFactoryEntity = fieldFactoryEntity;
        if (!this.playerViewPresenter.validate()) {
            throw new IllegalStateException("no player exist");
        }
        this.fieldFactoryEntity = fieldFactoryEntity;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.allFieldFactoryInfo = this.playerViewPresenter.findAvailableFieldFactoryInfo();
        this.fieldFactoryEntityForPlayer = this.playerViewPresenter.findFieldFactoryEntityByPlayer();
        this.availableFieldFactoryInfoForPlayer = this.playerViewPresenter.findAvailableFieldFactoryInfoByPlayer(
                this.playerViewPresenter.getPlayer().getLevel(),
                new LinkedHashSet<>(this.allFieldFactoryInfo),
                fieldFactoryEntityForPlayer
        );

        FormLayout form = new FormLayout();
        form.setSizeFull();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        fieldFactoryInfoEntityComboBox = new ComboBox<>();
        fieldFactoryInfoEntityComboBox.setAllowCustomValue(false);
        fieldFactoryInfoEntityComboBox.setItemLabelGenerator(FieldFactoryInfoEntity::getCategory);
        fieldFactoryInfoEntityComboBox.setItems(availableFieldFactoryInfoForPlayer);
        if (this.fieldFactoryEntity != null) {
            this.binder.setBean(this.fieldFactoryEntity);
            this.createModeValueSignal.set(false);
            fieldFactoryInfoEntityComboBox.setReadOnly(true);
            binder.forField(fieldFactoryInfoEntityComboBox)
                    .asRequired()
                    .bindReadOnly(FieldFactoryEntity::getFieldFactoryInfoEntity);
        } else {
            this.fieldFactoryEntity = new FieldFactoryEntity();
            this.binder.readBean(new FieldFactoryEntity());
            binder.forField(fieldFactoryInfoEntityComboBox)
                    .asRequired()
                    .bind(FieldFactoryEntity::getFieldFactoryInfoEntity, FieldFactoryEntity::setFieldFactoryInfoEntity);
        }
        fieldFactoryInfoEntityComboBox.bindValue(
                fieldFactoryInfoEntityValueSignal, fieldFactoryInfoEntityValueSignal::set
        );

        producingLengthIntegerField = new IntegerField();
        producingLengthIntegerField.bindVisible(fieldFactoryInfoEntityValueSignal.map(Objects::nonNull));
        producingLengthIntegerField.setPlaceholder("Producing Length");
        producingLengthIntegerField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        this.producingLengthSignal = binder.forField(producingLengthIntegerField)
                .asRequired()
                .withValidator(
                        (value1, context1) -> {
                            FieldFactoryInfoEntity fieldFactoryInfoEntity1 = fieldFactoryInfoEntityValueSignal.get();
                            if (fieldFactoryInfoEntity1 == null) {
                                return ValidationResult.error("fieldFactoryInfoEntity is unknow");
                            }
                            return new IntegerRangeValidator(
                                    "producing length should be %d-%d".formatted(
                                            fieldFactoryInfoEntity1.getDefaultProducingCapacity(),
                                            fieldFactoryInfoEntity1.getMaxProducingCapacity()
                                    ),
                                    fieldFactoryInfoEntity1.getDefaultProducingCapacity(),
                                    fieldFactoryInfoEntity1.getMaxProducingCapacity()
                            ).apply(value1, context1);
                        }
                )
                .bind(FieldFactoryEntity::getProducingLength, FieldFactoryEntity::setProducingLength).valueSignal();

        reapWindowSizeIntegerField = new IntegerField();
        reapWindowSizeIntegerField.bindVisible(fieldFactoryInfoEntityValueSignal.map(Objects::nonNull));
        reapWindowSizeIntegerField.setPlaceholder("Reap Window Size");
        reapWindowSizeIntegerField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        this.reapWindowSignal = binder.forField(reapWindowSizeIntegerField)
                .asRequired()
                .withValidator(
                        (value, context) -> {
                            FieldFactoryInfoEntity fieldFactoryInfoEntity = fieldFactoryInfoEntityValueSignal.peek();
                            if (fieldFactoryInfoEntity == null) {
                                return ValidationResult.error("fieldFactoryInfoEntity is unknow");
                            }
                            return new IntegerRangeValidator(
                                    "reap window size should be %d-%d".formatted(
                                            fieldFactoryInfoEntity.getDefaultReapWindowCapacity(),
                                            fieldFactoryInfoEntity.getMaxReapWindowCapacity()
                                    ),
                                    fieldFactoryInfoEntity.getDefaultReapWindowCapacity(),
                                    fieldFactoryInfoEntity.getMaxReapWindowCapacity()
                            ).apply(value, context);
                        }
                )
                .bind(FieldFactoryEntity::getReapWindowSize, FieldFactoryEntity::setReapWindowSize).valueSignal();

//        Signal.effect(
//                producingLengthIntegerField,
//                () -> {
//                    if (fieldFactoryInfoEntityValueSignal.get() != null && FieldFactoryInfoEntity.NULL_EMPTY_VALUE.equals(fieldFactoryInfoEntityValueSignal.get())) {
//                        producingLengthSignal.set(fieldFactoryInfoEntityValueSignal.get().getDefaultProducingCapacity());
//                    }
//                }
//        );
//        Signal.effect(
//                reapWindowSizeIntegerField,
//                () -> {
//                    if (fieldFactoryInfoEntityValueSignal.get() != null && FieldFactoryInfoEntity.NULL_EMPTY_VALUE.equals(fieldFactoryInfoEntityValueSignal.get())) {
//                        reapWindowSignal.set(fieldFactoryInfoEntityValueSignal.get().getDefaultReapWindowCapacity());
//                    }
//                }
//        );


        Checkbox checkboxOnlyAvailable = new Checkbox("Only Available");
        checkboxOnlyAvailable.bindValue(
                availableFieldFactoryBooleanValueSignal,
                availableFieldFactoryBooleanValueSignal::set
        );
        form.addFormItem(fieldFactoryInfoEntityComboBox, "Factory Type");
        form.addFormItem(producingLengthIntegerField, "Producing Length");
        form.addFormItem(reapWindowSizeIntegerField, "Reap Window Size");

        getContent().add(checkboxOnlyAvailable);
        getContent().addAndExpand(form);
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout verticalLayout = super.initContent();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        return verticalLayout;
    }


    public boolean submit(boolean update) {
        BinderValidationStatus<FieldFactoryEntity> result = this.binder.validate();
        if (result.hasErrors()) {
            return false;
        }

        boolean beanIfValid = this.binder.writeBeanIfValid(this.fieldFactoryEntity);
        if (beanIfValid) {
            if (update) {
                this.playerViewPresenter.updateFieldFactory(this.fieldFactoryEntity);
            } else {
                this.playerViewPresenter.saveFieldFactory(this.fieldFactoryEntity);
            }
        }
        return beanIfValid;
    }

}
