package com.careerfit.identity.development;

import com.careerfit.identity.CurrentUser;
import com.careerfit.identity.UserId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** M0 개발과 테스트에서만 사용하는 고정 사용자 fixture다. */
public final class DevelopmentUsers {

    public static final CurrentUser USER_A =
            new CurrentUser(new UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    public static final CurrentUser USER_B =
            new CurrentUser(new UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")));

    private static final Map<String, CurrentUser> USERS =
            Map.of("user-a", USER_A, "user-b", USER_B);

    private DevelopmentUsers() {}

    public static Optional<CurrentUser> findByAlias(String alias) {
        if (alias == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(USERS.get(alias));
    }
}
