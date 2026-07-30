package com.careerfit.identity.security;

import com.careerfit.identity.CurrentUser;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.RequestCurrentUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

@Component
@Primary
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    private final RequestCurrentUserContext fixtureContext;

    public SecurityContextCurrentUserProvider(RequestCurrentUserContext fixtureContext) {
        this.fixtureContext = fixtureContext;
    }

    @Override
    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return fixtureContext.currentUser();
        }
        return new CurrentUser(principal.userId());
    }
}
