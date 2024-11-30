package zzk.townshipscheduler.ui.views;

import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.*;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import zzk.townshipscheduler.backend.TownshipAuthenticationContext;

@Route("/login")
@AnonymousAllowed
public class LoginView
        extends LoginOverlay
        implements BeforeEnterObserver, AfterNavigationObserver {

    private final TownshipAuthenticationContext townshipAuthenticationContext;

    public LoginView(TownshipAuthenticationContext townshipAuthenticationContext) {
        this.townshipAuthenticationContext = townshipAuthenticationContext;

        setAction(
                RouteUtil.getRoutePath(
                        VaadinService.getCurrent().getContext(), getClass()
                )
        );

        LoginI18n i18n = LoginI18n.createDefault();
        i18n.setHeader(new LoginI18n.Header());
        i18n.getHeader().setTitle("Township Scheduler");
        i18n.getHeader().setDescription("Login using test/test");
        i18n.setAdditionalInformation(null);
        setI18n(i18n);

        setForgotPasswordButtonVisible(false);
        setOpened(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (townshipAuthenticationContext.isUserLoggedIn()) {
            event.forwardTo(HelloWorld.class);
        } else {
            setOpened(true);
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }

}
