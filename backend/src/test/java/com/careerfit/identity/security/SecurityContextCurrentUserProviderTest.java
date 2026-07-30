package com.careerfit.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.identity.UnauthenticatedUserException;
import com.careerfit.identity.RequestCurrentUserContext;
import com.careerfit.identity.UserId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("SecurityContext 현재 사용자 제공자 테스트")
class SecurityContextCurrentUserProviderTest {

    private final SecurityContextCurrentUserProvider provider =
            new SecurityContextCurrentUserProvider(new RequestCurrentUserContext());

    @AfterEach
    void 보안_컨텍스트를_초기화한다() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SecurityContext의 인증 주체에서 사용자 ID를 제공한다")
    void 보안_컨텍스트의_인증_주체에서_사용자_ID를_제공한다() {
        UserId userId = new UserId(UUID.randomUUID());
        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(userId, "user@example.com", "{noop}secret", true);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()));

        assertThat(provider.currentUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("인증 정보가 없으면 현재 사용자를 제공하지 않는다")
    void 인증_정보가_없으면_현재_사용자를_제공하지_않는다() {
        assertThatThrownBy(provider::currentUser)
                .isInstanceOf(UnauthenticatedUserException.class);
    }
}
