package com.careerfit.identity.security;

import com.careerfit.identity.CurrentUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** 통합 테스트에서 실제 인증 주체를 SecurityContext에 설정하고 원래 상태를 복원한다. */
public final class SecurityContextUserScope implements AutoCloseable {

    private final SecurityContext previousContext;

    private SecurityContextUserScope(CurrentUser user) {
        previousContext = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                user.userId(),
                user.userId().value() + "@ownership.test",
                "{noop}unused",
                true);
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, principal.passwordHash(), principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }

    public static SecurityContextUserScope authenticate(CurrentUser user) {
        return new SecurityContextUserScope(user);
    }

    @Override
    public void close() {
        SecurityContextHolder.setContext(previousContext);
    }
}
