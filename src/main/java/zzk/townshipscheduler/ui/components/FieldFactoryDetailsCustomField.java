package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import zzk.townshipscheduler.backend.persistence.FieldFactoryEntity;
import zzk.townshipscheduler.backend.persistence.FieldFactoryInfoEntity;

public class FieldFactoryDetailsCustomField extends CustomField<FieldFactoryEntity.FieldFactoryDetails> {

    private IntegerField producingLengthField;

    private IntegerField reapWindowsSizeField;

    public FieldFactoryDetailsCustomField() {
        setupProducingLengthField();
        setupReapWindowField();
        add(
                new HorizontalLayout(
                        this.producingLengthField,
                        this.reapWindowsSizeField
                )
        );
    }

    public FieldFactoryDetailsCustomField(FieldFactoryEntity.FieldFactoryDetails fieldFactoryDetails) {
        this();
        producingLengthField.setValue(fieldFactoryDetails.getProducingLength());
        reapWindowsSizeField.setValue(fieldFactoryDetails.getReapWindowSize());
    }

    private void setupReapWindowField() {
        IntegerField integerField = new IntegerField();
        integerField.setPlaceholder("Reap Window Size");
        this.reapWindowsSizeField = integerField;
    }

    private void setupProducingLengthField() {
        IntegerField integerField = new IntegerField();
        integerField.setPlaceholder("Producing Length");
        this.producingLengthField = integerField;
    }

    @Override
    protected FieldFactoryEntity.FieldFactoryDetails generateModelValue() {
        return new FieldFactoryEntity.FieldFactoryDetails(
                this.producingLengthField.getValue(),
                this.reapWindowsSizeField.getValue()
        );
    }

    @Override
    protected void setPresentationValue(FieldFactoryEntity.FieldFactoryDetails fieldFactoryDetails) {
        int producingLength = fieldFactoryDetails.getProducingLength();
        int reapWindowSize = fieldFactoryDetails.getReapWindowSize();
        this.producingLengthField.setValue(producingLength);
        this.reapWindowsSizeField.setValue(reapWindowSize);
    }

}
