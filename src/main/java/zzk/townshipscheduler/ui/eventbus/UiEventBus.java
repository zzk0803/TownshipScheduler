package zzk.townshipscheduler.ui.eventbus;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class UiEventBus {

    private UiEventBus() {

    }

    public static void publish(final ComponentEvent<?> componentEvent) {
        ComponentUtil.fireEvent(UI.getCurrent(), componentEvent);
    }

    public static <T extends ComponentEvent<?>> void subscribe(
            final Component componentEventSubscriberComponent,
            final Class<T> componentEventType,
            final ComponentEventSubscriber<T> componentEventSubscriber
    ) {
        final var registrationReference = new AtomicReference<Registration>();

        componentEventSubscriberComponent.addAttachListener(attachEvent -> {
            final Registration registration = ComponentUtil.addListener(
                    UI.getCurrent(),
                    componentEventType,
                    componentEventSubscriber::onComponentEvent
            );
            registrationReference.set(registration);
        });

        componentEventSubscriberComponent.addDetachListener(detachEvent -> {
            Optional.ofNullable(registrationReference.get()).ifPresent(Registration::remove);
        });
    }

    @FunctionalInterface
    public interface ComponentEventSubscriber<T> {

        void onComponentEvent(T componentEvent);

    }

}
