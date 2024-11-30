package zzk.townshipscheduler.ui.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route("/")
@AnonymousAllowed
public class HelloWorld extends Main {

    public HelloWorld() {
        addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.Height.FULL,
                LumoUtility.JustifyContent.CENTER,
                LumoUtility.AlignItems.CENTER
        );
        add(new H1("HELLO WORLD"));
    }

}
