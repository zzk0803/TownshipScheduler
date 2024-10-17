package zzk.project.vaadinproject.ui.components;//package zzk.project.zzk.project.vaadinproject.ui.components;
//
//import com.vaadin.flow.component.*;
//import com.vaadin.flow.component.dependency.JsModule;
//import com.vaadin.flow.shared.Registration;
//
//import java.util.List;
//
//@Tag("lit-vis-timeline")
//@JsModule("./src/scheduling/lit-vis-timeline.ts")
//public class LitVisTimeline
//        extends Component {
//
//    private static PropertyDescriptor<String, String> DATASETS_PD
//            = PropertyDescriptors.propertyWithDefault("datasets", "[]");
//
//    public LitVisTimeline() {
//    }
//
//    public String getDatasets() {
//        return DATASETS_PD.get(this);
//    }
//
//    public void setDatasets(String value) {
//        DATASETS_PD.set(this, value);
//    }
//
//    public Registration addComponentEventListener(ComponentEventListener<LitVisTimelineEvent> listener) {
//        return addListener(LitVisTimelineEvent.class, listener);
//    }
//
//    /*
//    You can fire an event on the server by creating the event instance
//    and passing it to the fireEvent() method.
//    Use false as the second constructor parameter
//    to specify that the event doesn’t come from the client.
//     */
//    public void setItems(List<String> items) {
//        getElement().setProperty("datasets", String.join(",", items));
//        fireEvent(new LitVisTimelineEvent(this, false));
//    }
//
//
//}
