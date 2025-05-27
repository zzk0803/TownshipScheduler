package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.service.PlayerService;
import zzk.townshipscheduler.ui.utility.UiEventBus;

@Slf4j
class PlayerFieldFactoryArticleForm extends Composite<VerticalLayout> {

//    private final PlayerService playerService;
//
//    private final PlayerEntity playerEntity;

    private final Binder<FieldFactoryEntity> binder;

    private final ComboBox<FieldFactoryInfoEntity> fieldFactoryInfoEntityComboBox;

    private final IntegerField producingLengthIntegerField;

    private final IntegerField reapWindowSizeIntegerField;

    private transient FieldFactoryEntity fieldFactoryEntity;

    private transient FieldFactoryInfoEntity fieldFactoryInfoEntity;

    public PlayerFieldFactoryArticleForm(
            PlayerEntity playerEntity,
            PlayerService playerService
    ) {
//        this.playerEntity = playerEntity;
//        this.playerService = playerService;
        this.binder = new Binder<>(FieldFactoryEntity.class);
        this.fieldFactoryEntity = new FieldFactoryEntity();

        this.binder.setBean(fieldFactoryEntity);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        producingLengthIntegerField = new IntegerField();
        producingLengthIntegerField.setPlaceholder("Producing Length");
        reapWindowSizeIntegerField = new IntegerField();
        reapWindowSizeIntegerField.setPlaceholder("Reap Window Size");
        fieldFactoryInfoEntityComboBox = new ComboBox<>();
        fieldFactoryInfoEntityComboBox.setItems(playerService.findAvailableFieldFactoryInfoByPlayer(playerEntity));
        fieldFactoryInfoEntityComboBox.setAllowCustomValue(false);
        fieldFactoryInfoEntityComboBox.setItemLabelGenerator(FieldFactoryInfoEntity::getCategory);
        fieldFactoryInfoEntityComboBox.addValueChangeListener(
                event -> {
                    FieldFactoryInfoEntity factoryInfo = event.getValue();
                    FieldFactoryInfoEntity oldValue = event.getOldValue();
                    if (factoryInfo != oldValue) {
                        this.fieldFactoryInfoEntity = factoryInfo;
                        producingLengthIntegerField.clear();
                        reapWindowSizeIntegerField.clear();
                        producingLengthIntegerField.setMin(1);
                        producingLengthIntegerField.setMax(factoryInfo.getMaxProducingCapacity());
                        reapWindowSizeIntegerField.setMin(1);
                        reapWindowSizeIntegerField.setMax(factoryInfo.getMaxReapWindowCapacity());
                    }

                }
        );

        UiEventBus.subscribe(
                this,
                PlayerFieldFactoryPersistRequestEvent.class,
                request -> {
                    playerService.saveFieldFactory(fieldFactoryEntity, playerEntity);
                }
        );

        form.addFormItem(fieldFactoryInfoEntityComboBox, "Factory Type");
        form.addFormItem(producingLengthIntegerField, "Producing Length");
        form.addFormItem(reapWindowSizeIntegerField, "Reap Window Size");

        this.binder.forField(fieldFactoryInfoEntityComboBox)
                .asRequired()
                .bind(
                        FieldFactoryEntity::getFieldFactoryInfoEntity,
                        FieldFactoryEntity::setFieldFactoryInfoEntity
                );
        this.binder.forField(producingLengthIntegerField)
                .asRequired()
                .withValidator(new IntegerRangeValidator(
                        "producing length should be %d-%d".formatted(
                                fieldFactoryInfoEntity.getDefaultProducingCapacity(),
                                fieldFactoryInfoEntity.getMaxProducingCapacity()
                        ),
                        fieldFactoryInfoEntity.getDefaultProducingCapacity(),
                        fieldFactoryInfoEntity.getMaxProducingCapacity()
                ))
                .bind(
                        FieldFactoryEntity::getProducingLength,
                        FieldFactoryEntity::setProducingLength
                );
        this.binder.forField(reapWindowSizeIntegerField)
                .asRequired()
                .withValidator(new IntegerRangeValidator(
                        "reap window size should be %d-%d".formatted(
                                fieldFactoryInfoEntity.getDefaultReapWindowCapacity(),
                                fieldFactoryInfoEntity.getMaxReapWindowCapacity()
                        ),
                        fieldFactoryInfoEntity.getDefaultReapWindowCapacity(),
                        fieldFactoryInfoEntity.getMaxReapWindowCapacity()
                ))
                .bind(
                        FieldFactoryEntity::getReapWindowSize,
                        FieldFactoryEntity::setReapWindowSize
                );

        getContent().add(form);
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout verticalLayout = super.initContent();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);
        verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        return verticalLayout;
    }


}
