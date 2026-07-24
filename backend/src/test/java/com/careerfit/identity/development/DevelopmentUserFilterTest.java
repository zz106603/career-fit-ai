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

    private final RequestCurrentUserContext currentUserContext = new RequestCurrentUserContext();
    private final DevelopmentUserFilter filter = new DevelopmentUserFilter(currentUserContext);

    @Test
    @DisplayName("사용자 헤더가 없으면 요청을 거절한다")
    void 사용자_헤더가_없으면_요청을_거절한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new AssertionError("필터 체인이 실행되면 안 됩니다.");
        });

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("등록되지 않은 개발 사용자는 요청을 거절한다")
    void 등록되지_않은_개발_사용자는_요청을_거절한다() throws Exception {
        MockHttpServletRequest request = requestWithUser("unknown-user");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new AssertionError("필터 체인이 실행되면 안 됩니다.");
        });

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("개발 사용자를 요청부터 서비스 호출까지 전달하고 종료 후 제거한다")
    void 개발_사용자를_요청부터_서비스_호출까지_전달하고_종료_후_제거한다() throws Exception {
        AtomicReference<UserId> propagatedUser = new AtomicReference<>();

        filter.doFilter(
                requestWithUser("user-a"),
                new MockHttpServletResponse(),
                (request, response) -> propagatedUser.set(currentUserContext.currentUserId()));

        assertThat(propagatedUser.get()).isEqualTo(DevelopmentUsers.USER_A.userId());
        assertThatThrownBy(currentUserContext::currentUser)
                .isInstanceOf(UnauthenticatedUserException.class);
    }

    @Test
    @DisplayName("요청마다 서로 다른 개발 사용자로 전환할 수 있다")
    void 요청마다_서로_다른_개발_사용자로_전환할_수_있다() throws Exception {
        AtomicReference<UserId> firstUser = new AtomicReference<>();
        AtomicReference<UserId> secondUser = new AtomicReference<>();

        filter.doFilter(
                requestWithUser("user-a"),
                new MockHttpServletResponse(),
                (request, response) -> firstUser.set(currentUserContext.currentUserId()));
        filter.doFilter(
                requestWithUser("user-b"),
                new MockHttpServletResponse(),
                (request, response) -> secondUser.set(currentUserContext.currentUserId()));

        assertThat(firstUser.get()).isEqualTo(DevelopmentUsers.USER_A.userId());
        assertThat(secondUser.get()).isEqualTo(DevelopmentUsers.USER_B.userId());
    }

    private MockHttpServletRequest requestWithUser(String userAlias) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DevelopmentUserFilter.USER_HEADER, userAlias);
        return request;
    }
}
