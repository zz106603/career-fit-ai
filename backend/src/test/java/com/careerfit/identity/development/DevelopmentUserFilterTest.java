package com.careerfit.identity.development;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.identity.RequestCurrentUserContext;
import com.careerfit.identity.UnauthenticatedUserException;
import com.careerfit.identity.UserId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("개발 사용자 필터 테스트")
class DevelopmentUserFilterTest {

    private final RequestCurrentUserContext 사용자_컨텍스트 = new RequestCurrentUserContext();
    private final DevelopmentUserFilter 필터 = new DevelopmentUserFilter(사용자_컨텍스트);

    @Test
    @DisplayName("사용자 헤더가 없으면 요청을 거절한다")
    void 사용자_헤더가_없으면_요청을_거절한다() throws Exception {
        MockHttpServletRequest 요청 = new MockHttpServletRequest();
        MockHttpServletResponse 응답 = new MockHttpServletResponse();

        필터.doFilter(요청, 응답, (request, response) -> {
            throw new AssertionError("필터 체인이 실행되면 안 됩니다.");
        });

        assertThat(응답.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("등록되지 않은 개발 사용자는 요청을 거절한다")
    void 등록되지_않은_개발_사용자는_요청을_거절한다() throws Exception {
        MockHttpServletRequest 요청 = 요청("unknown-user");
        MockHttpServletResponse 응답 = new MockHttpServletResponse();

        필터.doFilter(요청, 응답, (request, response) -> {
            throw new AssertionError("필터 체인이 실행되면 안 됩니다.");
        });

        assertThat(응답.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("개발 사용자를 요청부터 서비스 호출까지 전달하고 종료 후 제거한다")
    void 개발_사용자를_요청부터_서비스_호출까지_전달하고_종료_후_제거한다() throws Exception {
        AtomicReference<UserId> 전달된_사용자 = new AtomicReference<>();

        필터.doFilter(
                요청("user-a"),
                new MockHttpServletResponse(),
                (request, response) -> 전달된_사용자.set(사용자_컨텍스트.currentUserId()));

        assertThat(전달된_사용자.get()).isEqualTo(DevelopmentUsers.USER_A.userId());
        assertThatThrownBy(사용자_컨텍스트::currentUser)
                .isInstanceOf(UnauthenticatedUserException.class);
    }

    @Test
    @DisplayName("요청마다 서로 다른 개발 사용자로 전환할 수 있다")
    void 요청마다_서로_다른_개발_사용자로_전환할_수_있다() throws Exception {
        AtomicReference<UserId> 첫_사용자 = new AtomicReference<>();
        AtomicReference<UserId> 두번째_사용자 = new AtomicReference<>();

        필터.doFilter(
                요청("user-a"),
                new MockHttpServletResponse(),
                (request, response) -> 첫_사용자.set(사용자_컨텍스트.currentUserId()));
        필터.doFilter(
                요청("user-b"),
                new MockHttpServletResponse(),
                (request, response) -> 두번째_사용자.set(사용자_컨텍스트.currentUserId()));

        assertThat(첫_사용자.get()).isEqualTo(DevelopmentUsers.USER_A.userId());
        assertThat(두번째_사용자.get()).isEqualTo(DevelopmentUsers.USER_B.userId());
    }

    private MockHttpServletRequest 요청(String 사용자_별칭) {
        MockHttpServletRequest 요청 = new MockHttpServletRequest();
        요청.addHeader(DevelopmentUserFilter.USER_HEADER, 사용자_별칭);
        return 요청;
    }
}
