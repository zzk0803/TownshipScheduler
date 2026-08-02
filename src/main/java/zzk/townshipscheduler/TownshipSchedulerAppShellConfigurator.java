package zzk.townshipscheduler;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.lumo.Lumo;

import java.io.Serial;

@Push
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet(value = "styles.css")
public class TownshipSchedulerAppShellConfigurator
        implements AppShellConfigurator {

    @Serial
    private static final long serialVersionUID = 1046550348172640564L;

}
