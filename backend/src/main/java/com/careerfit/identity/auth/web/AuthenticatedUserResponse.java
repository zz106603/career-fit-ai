package com.careerfit.identity.auth.web;

import com.careerfit.identity.account.domain.UserAccount;
import com.careerfit.identity.security.AuthenticatedUserPrincipal;
import java.util.UUID;

public record AuthenticatedUserResponse(UUID userId, String email) {

    static AuthenticatedUserResponse from(UserAccount account) {
        return new AuthenticatedUserResponse(account.id().value(), account.email());
    }

    static AuthenticatedUserResponse from(AuthenticatedUserPrincipal principal) {
        return new AuthenticatedUserResponse(principal.userId().value(), principal.email());
    }
}
