package zzk.townshipscheduler.backend;

import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import zzk.townshipscheduler.backend.persistence.AccountEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TownshipAuthenticationContext {

    private final AuthenticationContext authenticationContext;

    public Optional<PlayerEntity> getPlayerEntity() {
        return Optional.ofNullable(getUserDetails()).map(AccountEntity::getPlayerEntity);
    }

    public AccountEntity getUserDetails() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof AccountEntity accountEntity) {
            return accountEntity;
        }else {
            return null;
        }
    }

    public String getUsername() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return switch (principal) {
            case AccountEntity accountEntity -> accountEntity.getUsername();
            case null, default -> ""; // Anonymous or no authentication.
        };
    }

    public boolean isUserLoggedIn() {
        return isUserLoggedIn(SecurityContextHolder.getContext().getAuthentication());
    }

    private boolean isUserLoggedIn(Authentication authentication) {
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public void logout() {
        var request = VaadinServletRequest.getCurrent().getHttpServletRequest();

        authenticationContext.logout();

        var cookie = new Cookie("remember-me", null);
        cookie.setMaxAge(0);
        cookie.setPath(StringUtils.hasLength(request.getContextPath()) ? request.getContextPath() : "/");

        var response = (HttpServletResponse) VaadinResponse.getCurrent();
        response.addCookie(cookie);
    }

}
