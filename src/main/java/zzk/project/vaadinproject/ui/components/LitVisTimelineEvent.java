package zzk.project.vaadinproject.ui.components;//package zzk.project.zzk.project.vaadinproject.ui.components;
//
//import com.vaadin.flow.component.ComponentEvent;
//import com.vaadin.flow.component.ComponentEventListener;
//import com.vaadin.flow.component.DomEvent;
//import com.vaadin.flow.component.EventData;
//import com.vaadin.flow.shared.Registration;
//
/// *
//    docs:https://vaadin.com/docs/latest/flow/create-ui/creating-components/events
//
//    You can connect a component event to a DOM event
//    that’s fired by the element in the browser.
//
//    To do this, use the @DomEvent annotation in your event class
//    to specify the name of the DOM event to listen to.
//
//    Vaadin Flow automatically adds a DOM event listener to the element
//    when a component event listener is present.
//
//    ---
//
//    Instead of sending all DOM events to the server,
//    you can filter events by defining a filter in the @DomEvent annotation.
//    The filter is typically based on things related to the event.
// */
//@DomEvent(value = "click",filter = "click.dbclick")
//public class LitVisTimelineEvent
//        extends ComponentEvent<LitVisTimeline> {
//
//    /*
//    An event can include additional information,
//    for example the mouse button used in a click event.
//
//    The @DomEvent annotation supports additional constructor parameters.
//
//    You can use the @EventData annotation to specify which data to send from the browser.
//
//
//     */
//    public LitVisTimelineEvent(
//            LitVisTimeline source,
//            boolean fromClient,
//            @EventData("event.button") int button
//    ) {
//        super(source, fromClient);
//    }
//
//}
