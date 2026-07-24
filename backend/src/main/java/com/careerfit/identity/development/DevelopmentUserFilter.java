package com.careerfit.identity.development;

import com.careerfit.identity.CurrentUser;
import com.careerfit.identity.RequestCurrentUserContext;
import com.careerfit.identity.RequestCurrentUserContext.UserScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** local/test 프로필의 요청을 고정 개발 사용자 컨텍스트로 변환한다. */
@Component
@Profile({"local", "test"})
public final class DevelopmentUserFilter extends OncePerRequestFilter {

    public static final String USER_HEADER = "X-Development-User";

    private final RequestCurrentUserContext currentUserContext;

    public DevelopmentUserFilter(RequestCurrentUserContext currentUserContext) {
        this.currentUserContext = currentUserContext;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CurrentUser user =
                DevelopmentUsers.findByAlias(request.getHeader(USER_HEADER)).orElse(null);
        if (user == null) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED, "유효한 개발 사용자 컨텍스트가 필요합니다.");
            return;
        }

        try (UserScope ignored = currentUserContext.bind(user)) {
            filterChain.doFilter(request, response);
        }
    }
}
