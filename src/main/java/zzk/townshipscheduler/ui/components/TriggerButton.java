package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class TriggerButton extends Composite<HorizontalLayout> {

    private Button state1Btn;

    private Button state2Btn;

    public TriggerButton(
            String state1BtnLabel,
            ComponentEventListener<ClickEvent<Button>> state1BtnClicked,
            String state2BtnLabel,
            ComponentEventListener<ClickEvent<Button>> state2BtnClicked
    ) {
        state1Btn = new Button();
        state1Btn.setText(state1BtnLabel);
        state1Btn.addClickListener(buttonClickEvent -> {
            fromState1ToState2();
        });
        state1Btn.addClickListener(state1BtnClicked);

        state2Btn = new Button();
        state2Btn.setVisible(false);
        state2Btn.setText(state2BtnLabel);
        state2Btn.addClickListener(buttonClickEvent -> {
            fromState2ToState1();
        });
        state2Btn.addClickListener(state2BtnClicked);

        getContent().add(state1Btn, state2Btn);
    }

    public void fromState1ToState2() {
        state1Btn.setDisableOnClick(true);
        state1Btn.setVisible(false);
        state2Btn.setVisible(true);
    }

    public void fromState2ToState1() {
        state2Btn.setDisableOnClick(true);
        state1Btn.setVisible(true);
        state2Btn.setVisible(false);
    }

    @Override
    protected HorizontalLayout initContent() {
        return super.initContent();
    }

}
