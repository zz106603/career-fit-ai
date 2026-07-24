package com.careerfit.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.identity.RequestCurrentUserContext.UserScope;
import com.careerfit.identity.development.DevelopmentUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("요청 사용자 컨텍스트 테스트")
class RequestCurrentUserContextTest {

    private final RequestCurrentUserContext currentUserContext = new RequestCurrentUserContext();

    @Test
    @DisplayName("사용자가 없으면 보호된 호출을 거절한다")
    void 사용자가_없으면_보호된_호출을_거절한다() {
        assertThatThrownBy(currentUserContext::currentUser)
                .isInstanceOf(UnauthenticatedUserException.class);
    }

    @Test
    @DisplayName("바인딩한 사용자를 서비스 호출까지 제공하고 종료 후 제거한다")
    void 바인딩한_사용자를_서비스_호출까지_제공하고_종료_후_제거한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            assertThat(currentUserContext.currentUser()).isEqualTo(DevelopmentUsers.USER_A);
        }

        assertThatThrownBy(currentUserContext::currentUser)
                .isInstanceOf(UnauthenticatedUserException.class);
    }

    @Test
    @DisplayName("서로 다른 요청에서 사용자 fixture를 전환할 수 있다")
    void 서로_다른_요청에서_사용자_fixture를_전환할_수_있다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            assertThat(currentUserContext.currentUserId())
                    .isEqualTo(DevelopmentUsers.USER_A.userId());
        }
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_B)) {
            assertThat(currentUserContext.currentUserId())
                    .isEqualTo(DevelopmentUsers.USER_B.userId());
        }
    }
}
