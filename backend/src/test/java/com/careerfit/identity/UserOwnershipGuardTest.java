package com.careerfit.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.identity.RequestCurrentUserContext.UserScope;
import com.careerfit.identity.development.DevelopmentUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("사용자 소유권 보호 테스트")
class UserOwnershipGuardTest {

    private final RequestCurrentUserContext 사용자_컨텍스트 = new RequestCurrentUserContext();
    private final UserOwnershipGuard 소유권_보호 = new UserOwnershipGuard(사용자_컨텍스트);

    @Test
    @DisplayName("현재 사용자가 소유한 리소스 접근을 허용한다")
    void 현재_사용자가_소유한_리소스_접근을_허용한다() {
        try (UserScope ignored = 사용자_컨텍스트.bind(DevelopmentUsers.USER_A)) {
            assertThatCode(() -> 소유권_보호.verifyOwner(DevelopmentUsers.USER_A.userId()))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("다른 사용자가 소유한 리소스 접근을 거절한다")
    void 다른_사용자가_소유한_리소스_접근을_거절한다() {
        try (UserScope ignored = 사용자_컨텍스트.bind(DevelopmentUsers.USER_A)) {
            assertThatThrownBy(
                            () -> 소유권_보호.verifyOwner(DevelopmentUsers.USER_B.userId()))
                    .isInstanceOf(ResourceOwnershipException.class);
        }
    }

    @Test
    @DisplayName("사용자 컨텍스트 없이 소유권을 검사하면 거절한다")
    void 사용자_컨텍스트_없이_소유권을_검사하면_거절한다() {
        assertThatThrownBy(() -> 소유권_보호.verifyOwner(DevelopmentUsers.USER_A.userId()))
                .isInstanceOf(UnauthenticatedUserException.class);
    }
}
