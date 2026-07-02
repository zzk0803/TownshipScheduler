package zzk.townshipscheduler.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import lombok.Getter;
import lombok.Setter;

@JsModule("./components/benchmark-view.ts")
@Tag("benchmark-view")
public class LitBenchmarkView extends Component {

    @Getter
    @Setter
    private String content;

    public LitBenchmarkView(String content) {
        this.content = content;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getElement().setProperty("content", getContent());
    }

}
