package com.careerfit.identity.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerfit.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("회원가입과 세션 인증 통합 테스트")
class AuthFlowIntegrationTest extends PostgresIntegrationTest {

    private static final String EMAIL = "member@example.com";
    private static final String PASSWORD = "safe-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${server.servlet.session.cookie.http-only}")
    private boolean sessionCookieHttpOnly;

    @BeforeEach
    void 사용자_데이터를_초기화한다() {
        jdbcClient.sql("TRUNCATE user_account").update();
    }

    @Test
    @DisplayName("회원가입은 이메일을 정규화하고 비밀번호 해시만 저장한다")
    void 회원가입은_이메일을_정규화하고_비밀번호_해시만_저장한다() throws Exception {
        MvcResult signup = mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("  MEMBER@Example.COM  ", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andReturn();

        String storedHash = jdbcClient
                .sql("SELECT password_hash FROM user_account WHERE email = :email")
                .param("email", EMAIL)
                .query(String.class)
                .single();
        assertThat(storedHash).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, storedHash)).isTrue();

        MockHttpSession authenticatedSession =
                (MockHttpSession) signup.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    @DisplayName("정규화된 동일 이메일의 중복 회원가입은 공통 JSON 오류로 거부한다")
    void 정규화된_동일_이메일의_중복_회원가입은_거부한다() throws Exception {
        signup();

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("MEMBER@example.com", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("로그인하면 HttpOnly 세션 쿠키로 현재 사용자를 조회할 수 있다")
    void 로그인하면_HttpOnly_세션_쿠키로_현재_사용자를_조회할_수_있다() throws Exception {
        signup();

        MvcResult login = login(new MockHttpSession(), PASSWORD);
        MockHttpSession authenticatedSession =
                (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
        Cookie sessionCookie = login.getResponse().getCookie("JSESSIONID");
        if (sessionCookie != null) {
            assertThat(sessionCookie.isHttpOnly()).isTrue();
        }
        assertThat(sessionCookieHttpOnly).isTrue();
    }

    @Test
    @DisplayName("잘못된 비밀번호는 계정 존재 여부를 숨긴 동일한 인증 오류로 응답한다")
    void 잘못된_비밀번호는_동일한_인증_오류로_응답한다() throws Exception {
        signup();

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("unknown@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("비로그인 보호 API 요청은 리다이렉트 없이 공통 JSON 오류로 거부한다")
    void 비로그인_보호_API_요청은_공통_JSON_오류로_거부한다() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("CSRF 토큰 없는 상태 변경 요청은 공통 JSON 오류로 거부한다")
    void CSRF_토큰_없는_상태_변경_요청은_거부한다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(EMAIL, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("로그인하면 기존 세션 ID를 변경해 세션 고정 공격을 방어한다")
    void 로그인하면_기존_세션_ID를_변경한다() throws Exception {
        signup();
        MockHttpSession session = new MockHttpSession();
        String previousSessionId = session.getId();

        MvcResult login = login(session, PASSWORD);

        assertThat(login.getRequest().getSession(false).getId())
                .isNotEqualTo(previousSessionId);
    }

    @Test
    @DisplayName("로그아웃하면 기존 세션으로 보호 API에 접근할 수 없다")
    void 로그아웃하면_기존_세션으로_보호_API에_접근할_수_없다() throws Exception {
        signup();
        MockHttpSession session =
                (MockHttpSession) login(new MockHttpSession(), PASSWORD)
                        .getRequest()
                        .getSession(false);

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("CSRF 조회 API는 브라우저가 상태 변경 요청에 사용할 토큰을 제공한다")
    void CSRF_조회_API는_토큰을_제공한다() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("CSRF 조회 응답의 세션과 헤더 토큰으로 상태 변경 요청을 수행할 수 있다")
    void CSRF_조회_응답으로_상태_변경_요청을_수행할_수_있다() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String responseBody = csrfResult.getResponse().getContentAsString();
        String headerName = JsonPath.read(responseBody, "$.headerName");
        String token = JsonPath.read(responseBody, "$.token");
        MockHttpSession csrfSession =
                (MockHttpSession) csrfResult.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/signup")
                        .session(csrfSession)
                        .header(headerName, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("invalid", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private void signup() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(EMAIL, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private MvcResult login(MockHttpSession session, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(EMAIL, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static String credentials(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }
}
