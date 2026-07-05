package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;

@Slf4j
class PlayerFieldFactoryArticleForm
        extends Composite<VerticalLayout> {

    private final Binder<FieldFactoryEntity> binder = new Binder<>(FieldFactoryEntity.class);

    private final ComboBox<FieldFactoryInfoEntity> fieldFactoryInfoEntityComboBox;

    private final IntegerField producingLengthIntegerField;

    private final IntegerField reapWindowSizeIntegerField;

    private final PlayerViewPresenter playerViewPresenter;

    private ValueSignal<FieldFactoryInfoEntity> fieldFactoryInfoEntityValueSignal;

    private ValueSignal<Integer> producingLengthIntegerFieldSignal;

    private ValueSignal<Integer> reapWindowSizeIntegerFieldSignal;

    private FieldFactoryEntity fieldFactoryEntity;

    public PlayerFieldFactoryArticleForm(PlayerViewPresenter playerViewPresenter) {
        this.playerViewPresenter = playerViewPresenter;
        this.fieldFactoryEntity = new FieldFactoryEntity();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        producingLengthIntegerField = new IntegerField();
        producingLengthIntegerField.setPlaceholder("Producing Length");
        reapWindowSizeIntegerField = new IntegerField();
        reapWindowSizeIntegerField.setPlaceholder("Reap Window Size");
        fieldFactoryInfoEntityComboBox = new ComboBox<>();
        fieldFactoryInfoEntityComboBox.setAllowCustomValue(false);
        fieldFactoryInfoEntityComboBox.setItemLabelGenerator(FieldFactoryInfoEntity::getCategory);
        fieldFactoryInfoEntityComboBox.setItems(this.playerViewPresenter.findAvailableFieldFactoryInfoByPlayer());
        fieldFactoryInfoEntityComboBox.setRenderer(new ComponentRenderer<>(
                fieldFactoryInfoEntity -> {
                    return null;
                }));
        fieldFactoryInfoEntityComboBox.setValue(FieldFactoryInfoEntity.NULL_EMPTY_VALUE);
        setupBinder();

        Signal.effect(
                this,
                () -> {
                    if (
                            fieldFactoryInfoEntityValueSignal != null
                                    && fieldFactoryInfoEntityValueSignal.get() != null
                                    && !FieldFactoryInfoEntity.NULL_EMPTY_VALUE.equals(fieldFactoryInfoEntityValueSignal.get())
                    ) {
                        FieldFactoryInfoEntity selectedFieldFactoryType = fieldFactoryInfoEntityValueSignal.get();
                        producingLengthIntegerField.setMin(1);
                        producingLengthIntegerField.setMax(selectedFieldFactoryType.getMaxProducingCapacity());
                        reapWindowSizeIntegerField.setMin(1);
                        reapWindowSizeIntegerField.setMax(selectedFieldFactoryType.getMaxReapWindowCapacity());
                        producingLengthIntegerFieldSignal.set(selectedFieldFactoryType.getDefaultProducingCapacity());
                        reapWindowSizeIntegerFieldSignal.set(selectedFieldFactoryType.getDefaultReapWindowCapacity());
                    }
                }
        );

        form.addFormItem(fieldFactoryInfoEntityComboBox, "Factory Type");
        form.addFormItem(producingLengthIntegerField, "Producing Length");
        form.addFormItem(reapWindowSizeIntegerField, "Reap Window Size");

        getContent().add(form);
    }

    private Binder<FieldFactoryEntity> setupBinder() {
        binder.readBean(fieldFactoryEntity);
        Binder.Binding<FieldFactoryEntity, FieldFactoryInfoEntity> fieldFactoryInfoEntityBinding = binder.forField(fieldFactoryInfoEntityComboBox)
                .asRequired()
                .bind(
                        FieldFactoryEntity::getFieldFactoryInfoEntity,
                        FieldFactoryEntity::setFieldFactoryInfoEntity
                );
        fieldFactoryInfoEntityValueSignal = fieldFactoryInfoEntityBinding.valueSignal();
        Binder.Binding<FieldFactoryEntity, Integer> producingLengthBinding = binder.forField(producingLengthIntegerField)
                .asRequired()
                .withValidator(
                        (value, context) -> {
                            FieldFactoryInfoEntity fieldFactoryInfoEntity = fieldFactoryInfoEntityValueSignal.get();
                            return new IntegerRangeValidator(
                                    "producing length should be %d-%d".formatted(
                                            fieldFactoryInfoEntity.getDefaultProducingCapacity(),
                                            fieldFactoryInfoEntity.getMaxProducingCapacity()
                                    ),
                                    fieldFactoryInfoEntity.getDefaultProducingCapacity(),
                                    fieldFactoryInfoEntity.getMaxProducingCapacity()
                            ).apply(value, context);
                        }
                )
                .bind(
                        FieldFactoryEntity::getProducingLength,
                        FieldFactoryEntity::setProducingLength
                );
        producingLengthIntegerFieldSignal = producingLengthBinding.valueSignal();
        Binder.Binding<FieldFactoryEntity, Integer> reapWindowBinding = binder.forField(reapWindowSizeIntegerField)
                .asRequired()
                .withValidator((value, context) -> {
                    FieldFactoryInfoEntity fieldFactoryInfoEntity = fieldFactoryInfoEntityValueSignal.get();
                    return new IntegerRangeValidator(
                            "reap window size should be %d-%d".formatted(
                                    fieldFactoryInfoEntity.getDefaultReapWindowCapacity(),
                                    fieldFactoryInfoEntity.getMaxReapWindowCapacity()
                            ),
                            fieldFactoryInfoEntity.getDefaultReapWindowCapacity(),
                            fieldFactoryInfoEntity.getMaxReapWindowCapacity()
                    ).apply(value, context);
                })
                .bind(
                        FieldFactoryEntity::getReapWindowSize,
                        FieldFactoryEntity::setReapWindowSize
                );
        reapWindowSizeIntegerFieldSignal = reapWindowBinding.valueSignal();
        return binder;
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout verticalLayout = super.initContent();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);
        verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        return verticalLayout;
    }


    public void submit() {
        this.binder.writeBeanIfValid(new FieldFactoryEntity());
        FieldFactoryEntity savingEntity = this.binder.getBean();
        this.playerViewPresenter.saveFieldFactory(savingEntity);
    }

}
