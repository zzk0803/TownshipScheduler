package zzk.project.vaadinproject;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;

@Push
@Theme(value = "vaadin24p5")
@NpmPackage(value = "bootstrap",version = "^5.3.3")
public class TownshipSchedulerAppShelConfigurator
        implements AppShellConfigurator {

}
