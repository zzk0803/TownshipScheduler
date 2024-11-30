package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

public class BillDurationField
        extends CustomField<Duration> {

    private final TextField dayField;

    private final TextField hourField;

    private final TextField minuteField;

    public BillDurationField() {
        dayField = buildField("Day");
        hourField = buildField("Hour");
        minuteField = buildField("Minute");

        add(new HorizontalLayout(dayField, new Text("-"), hourField, new Text("-"), minuteField));
    }

    private TextField buildField(String part) {
        TextField textField = new TextField(part);
        textField.setPattern(Pattern.compile("\\d*").pattern());
        textField.setPlaceholder(part);
        return textField;
    }

    @Override
    protected Duration generateModelValue() {
        int day = formFieldToInt(dayField);
        int hour = formFieldToInt(hourField);
        int minute = formFieldToInt(minuteField);
        return Duration.ofDays(day).plusHours(hour).plusMinutes(minute);
    }

    private int formFieldToInt(TextField textField) {
        String value = textField.getValue();
        int i = Objects.isNull(value) || value.isEmpty() ? 0 : Integer.parseInt(value);
        return i < 0 ? 0 : i;
    }

    @Override
    protected void setPresentationValue(Duration duration) {
        if (duration != null) {
            dayField.setValue(String.valueOf(duration.toDaysPart()));
            hourField.setValue(String.valueOf(duration.toHoursPart()));
            minuteField.setValue(String.valueOf(duration.toMinutesPart()));
        } else {
            dayField.setValue("");
            hourField.setValue("");
            minuteField.setValue("");
        }
    }

}
