package zzk.townshipscheduler.ui.views.player;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
class PlayerFieldFactoryArticleForm
        extends VerticalLayout {

    private final PlayerViewPresenter playerViewPresenter;

    private final boolean createMode;

    private Map<FieldFactoryInfoEntity, Long> calcedPlayerPropertiesCountMap;

    private ValueSignal<FieldFactoryInfoEntity> selectedOrEditingFieldFactoryInfoValueSignal;

    private FieldFactoryEntity fieldFactoryEntity;

    private Binder<FieldFactoryEntity> binder;

    public PlayerFieldFactoryArticleForm(PlayerViewPresenter playerViewPresenter) {
        this.playerViewPresenter = playerViewPresenter;
        if (!this.playerViewPresenter.validate()) {
            throw new IllegalStateException("no player exist");
        }
        fieldFactoryEntity = new FieldFactoryEntity();
        createMode = true;
    }

    public PlayerFieldFactoryArticleForm(PlayerViewPresenter playerViewPresenter, FieldFactoryEntity fieldFactoryEntity) {
        this.playerViewPresenter = playerViewPresenter;
        this.fieldFactoryEntity = fieldFactoryEntity;
        if (!this.playerViewPresenter.validate()) {
            throw new IllegalStateException("no player exist");
        }
        this.fieldFactoryEntity = fieldFactoryEntity;
        createMode = false;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        removeAll();
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.START);
        setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        List<FieldFactoryInfoEntity> availableFieldFactoryInfoForPlayer = this.playerViewPresenter.findAvailableFieldFactoryInfoIfPlayerLevelExist();
        List<FieldFactoryEntity> fieldFactoryEntityForPlayer = this.playerViewPresenter.findFieldFactoryEntityByPlayer();
        this.calcedPlayerPropertiesCountMap = this.playerViewPresenter.calcPlayerPropertiesCount(
                fieldFactoryEntityForPlayer
        );

        Grid<FieldFactoryInfoEntity> fieldFactoryInfoEntityGrid = new Grid<>();
        fieldFactoryInfoEntityGrid.addClassName("player-field-factory-article-form");
        fieldFactoryInfoEntityGrid.addColumn(FieldFactoryInfoEntity::getCategory)
                .setHeader("Category");
        fieldFactoryInfoEntityGrid.addColumn(FieldFactoryInfoEntity::getLevel)
                .setHeader("Level");
        fieldFactoryInfoEntityGrid.addColumn(fieldFactoryInfoEntity -> {
                    return this.calcedPlayerPropertiesCountMap.getOrDefault(fieldFactoryInfoEntity, 0L) + "/" + fieldFactoryInfoEntity.getMaxInstanceAmount();
                })
                .setHeader("Instance You Have/Max Instance");
        fieldFactoryInfoEntityGrid.addColumn(FieldFactoryInfoEntity::getDefaultProducingCapacity)
                .setHeader("Default Producing Capacity");
        fieldFactoryInfoEntityGrid.addColumn(FieldFactoryInfoEntity::getMaxProducingCapacity)
                .setHeader("Max Producing Capacity");
        fieldFactoryInfoEntityGrid.addColumn(FieldFactoryInfoEntity::getDefaultReapWindowCapacity)
                .setHeader("Default Reap Window");
        fieldFactoryInfoEntityGrid.addColumn(FieldFactoryInfoEntity::getMaxReapWindowCapacity)
                .setHeader("Max Reap Window");
        fieldFactoryInfoEntityGrid.setItems(availableFieldFactoryInfoForPlayer);
        fieldFactoryInfoEntityGrid.setItemSelectableProvider(
                fieldFactoryInfoEntity -> calcedPlayerPropertiesCountMap.getOrDefault(fieldFactoryInfoEntity, 0L) < fieldFactoryInfoEntity.getMaxInstanceAmount()
        );
        fieldFactoryInfoEntityGrid.setPartNameGenerator(fieldFactoryInfoEntity -> {
            if (calcedPlayerPropertiesCountMap.getOrDefault(fieldFactoryInfoEntity, 0L) < fieldFactoryInfoEntity.getMaxInstanceAmount()) {
                return "field-factory-info-entity-accept";
            } else {
                return "field-factory-info-entity-reject";
            }
        });

        if (createMode) {
            selectedOrEditingFieldFactoryInfoValueSignal = new ValueSignal<>(FieldFactoryInfoEntity.NULL_EMPTY_VALUE);
            fieldFactoryInfoEntityGrid.asSingleSelect()
                    .bindValue(
                            selectedOrEditingFieldFactoryInfoValueSignal,
                            value -> {
                                var setToSignal = value == null
                                        ? FieldFactoryInfoEntity.NULL_EMPTY_VALUE
                                        : value;
                                selectedOrEditingFieldFactoryInfoValueSignal.set(setToSignal);
                            }
                    );
        } else {
            selectedOrEditingFieldFactoryInfoValueSignal = new ValueSignal<>(this.fieldFactoryEntity.getFieldFactoryInfoEntity());
            fieldFactoryInfoEntityGrid.select(this.fieldFactoryEntity.getFieldFactoryInfoEntity());
            fieldFactoryInfoEntityGrid.scrollToItem(this.fieldFactoryEntity.getFieldFactoryInfoEntity());
            fieldFactoryInfoEntityGrid.setSelectionMode(Grid.SelectionMode.NONE);
        }

        Signal<Boolean> fieldFactoryInfoSelectedSignal = selectedOrEditingFieldFactoryInfoValueSignal.map(obj -> Objects.nonNull(obj) && !FieldFactoryInfoEntity.NULL_EMPTY_VALUE.equals(obj));
        binder = new Binder<>(FieldFactoryEntity.class);
        TextField fieldFactoryTypeTextField = new TextField("Field&Factory Type");
        fieldFactoryTypeTextField.setReadOnly(true);
        fieldFactoryTypeTextField.bindValue(
                selectedOrEditingFieldFactoryInfoValueSignal.map(
                        fieldFactoryInfoEntity -> fieldFactoryInfoEntity != null | FieldFactoryInfoEntity.NULL_EMPTY_VALUE.equals(fieldFactoryInfoEntity)
                                ? fieldFactoryInfoEntity.getCategory()
                                : "N/A"),
                null
        );

        IntegerField producingLength = new IntegerField("Producing Length");
        Binder.Binding<FieldFactoryEntity, Integer> fieldFactoryEntityProducingLengthBinding
                = binder.forField(producingLength)
                .asRequired()
                .withValidator((value, context) -> {
                    FieldFactoryInfoEntity fieldFactoryInfoEntity = selectedOrEditingFieldFactoryInfoValueSignal.get();
                    if (fieldFactoryInfoEntity == null) {
                        return ValidationResult.error("field&factory type unknow");
                    }
                    if (value < 0 || value > fieldFactoryInfoEntity.getMaxProducingCapacity()) {
                        return ValidationResult.error("producing length should in (%d,%d]".formatted(0, fieldFactoryInfoEntity.getMaxProducingCapacity()));
                    }
                    return ValidationResult.ok();
                })
                .bind(FieldFactoryEntity::getProducingLength, FieldFactoryEntity::setProducingLength);
//        producingLengthIntegerSignal = fieldFactoryEntityProducingLengthBinding.valueSignal();
        producingLength.bindEnabled(fieldFactoryInfoSelectedSignal);
        producingLength.bindVisible(fieldFactoryInfoSelectedSignal);
        producingLength.bindMax(selectedOrEditingFieldFactoryInfoValueSignal.map(FieldFactoryInfoEntity::getMaxProducingCapacity));
        producingLength.bindMin(selectedOrEditingFieldFactoryInfoValueSignal.map(FieldFactoryInfoEntity::getDefaultProducingCapacity));
        if (createMode) {
            producingLength.setValue(Optional.ofNullable(selectedOrEditingFieldFactoryInfoValueSignal.peek())
                    .map(FieldFactoryInfoEntity::getDefaultProducingCapacity)
                    .orElse(3));
        }
        IntegerField reapWindow = new IntegerField("Reap Window");
        Binder.Binding<FieldFactoryEntity, Integer> fieldFactoryEntityReapWindowBinding
                = binder.forField(reapWindow)
                .asRequired()
                .withValidator((value, context) -> {
                    FieldFactoryInfoEntity fieldFactoryInfoEntity = selectedOrEditingFieldFactoryInfoValueSignal.get();
                    if (fieldFactoryInfoEntity == null) {
                        return ValidationResult.error("field&factory type unknow");
                    }
                    if (value < 0 || value > fieldFactoryInfoEntity.getMaxReapWindowCapacity()) {
                        return ValidationResult.error("reap window should in [%d,%d]".formatted(6, fieldFactoryInfoEntity.getMaxReapWindowCapacity()));
                    }
                    return ValidationResult.ok();
                })
                .bind(FieldFactoryEntity::getReapWindowSize, FieldFactoryEntity::setReapWindowSize);
//        reapWindowSizeIntegerSignal = fieldFactoryEntityReapWindowBinding.valueSignal();
        reapWindow.bindEnabled(fieldFactoryInfoSelectedSignal);
        reapWindow.bindVisible(fieldFactoryInfoSelectedSignal);
        reapWindow.bindMax(selectedOrEditingFieldFactoryInfoValueSignal.map(FieldFactoryInfoEntity::getMaxReapWindowCapacity));
        reapWindow.setMin(Optional.ofNullable(selectedOrEditingFieldFactoryInfoValueSignal.peek())
                .map(FieldFactoryInfoEntity::getDefaultReapWindowCapacity)
                .orElse(6));
        if (createMode) {
            reapWindow.setValue(Optional.ofNullable(selectedOrEditingFieldFactoryInfoValueSignal.peek())
                    .map(FieldFactoryInfoEntity::getDefaultReapWindowCapacity)
                    .orElse(6));
        }
        binder.readBean(fieldFactoryEntity);

        add(fieldFactoryInfoEntityGrid);
        add(fieldFactoryTypeTextField);
        add(new Hr());
        add(new HorizontalLayout(producingLength, reapWindow));

    }

    public boolean submit(boolean update) {
        FieldFactoryInfoEntity fieldFactoryInfoEntity = this.selectedOrEditingFieldFactoryInfoValueSignal.peek();
        if (fieldFactoryInfoEntity == null || FieldFactoryInfoEntity.NULL_EMPTY_VALUE.equals(fieldFactoryInfoEntity)) {
            return false;
        }

        this.fieldFactoryEntity.setFieldFactoryInfoEntity(fieldFactoryInfoEntity);
        BinderValidationStatus<FieldFactoryEntity> result = this.binder.validate();
        boolean hasErrors = result.hasErrors();
        //        || producingLengthIntegerSignal.peek() == null || reapWindowSizeIntegerSignal.peek() == null
        if (hasErrors) {
            return false;
        } else {
            try {
                this.binder.writeBean(this.fieldFactoryEntity);
            } catch (ValidationException e) {
                return false;
            }
            if (update) {
                this.playerViewPresenter.updateFieldFactory(this.fieldFactoryEntity);
            } else {
                this.playerViewPresenter.saveFieldFactory(this.fieldFactoryEntity);
            }
        }

        return true;
    }

}
