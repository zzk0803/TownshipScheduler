package zzk.townshipscheduler.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import zzk.townshipscheduler.backend.persistence.AccountEntity;

import java.util.List;

@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout implements ApplicationContextAware {

    private H1 viewTitle;

    private transient AuthenticationContext authenticationContext;

    public MainLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addDrawerContent() {
        Span appName = new Span("Township Scheduler");
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        HorizontalLayout headerWrapper = new HorizontalLayout();
        HorizontalLayout rightWrapper = new HorizontalLayout();
        headerWrapper.setWidthFull();
        headerWrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerWrapper.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        rightWrapper.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        headerWrapper.add(viewTitle);
        Button configButton = new Button(
                VaadinIcon.COG.create(), click -> {
            Notification.show("todo");
        }
        );
        configButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        rightWrapper.add(configButton);
        authenticationContext.getAuthenticatedUser(AccountEntity.class).ifPresentOrElse(
                appUserEntity -> {
                    Avatar avatar = new Avatar(appUserEntity.getName());
                    avatar.setThemeName("xsmall");
                    avatar.getElement().setAttribute("tabindex", "-1");

                    MenuBar userMenu = new MenuBar();
                    userMenu.setThemeName("tertiary-inline contrast");
                    MenuItem userName = userMenu.addItem("");

                    Div div = new Div();
                    div.add(avatar);
                    div.add(new Icon("lumo", "dropdown"));
                    div.getElement().getStyle().set("display", "flex");
                    div.getElement().getStyle().set("align-items", "center");
                    div.getElement().getStyle().set("gap", "var(--lumo-space-s)");
                    userName.add(div);
                    userName.getSubMenu().addItem(
                            "Sign out", e -> {
                                authenticationContext.logout();
                            }
                    );

                    rightWrapper.add(userMenu);

                }, () -> {
                    Button loginButton = new Button("Sign In");
                    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    loginButton.addClickListener(buttonClickEvent -> {
                        UI.getCurrent().navigate("login");
                    });
                    loginButton.getStyle().setMarginRight("2px");
                    rightWrapper.add(loginButton);
                }
        );

//        headerWrapper.add(new MainLayoutUserLoginStatefulComponent(this.authenticatedUser));

        headerWrapper.add(rightWrapper);
        addToNavbar(true, toggle, headerWrapper);
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        menuEntries.forEach(entry -> {
            if (entry.icon() != null) {
                nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
            } else {
                nav.addItem(new SideNavItem(entry.title(), entry.path()));
            }
        });

        return nav;
    }

    private Footer createFooter() {
        Footer footer = new Footer();

        return footer;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.authenticationContext = applicationContext.getBean(AuthenticationContext.class);
    }

}
