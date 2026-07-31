package com.careerfit.career.document.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.identity.CurrentUser;
import com.careerfit.identity.development.DevelopmentUsers;
import com.careerfit.identity.security.AuthenticatedUserPrincipal;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("경력 PDF 업로드 흐름 통합 테스트")
class CareerDocumentFlowIntegrationTest extends PostgresIntegrationTest {

    private static final Path STORAGE_ROOT =
            Path.of("build", "test-career-document-storage").toAbsolutePath();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("career-fit.storage.local.root", STORAGE_ROOT::toString);
    }

    @BeforeEach
    void 문서_데이터와_파일을_초기화한다() throws Exception {
        jdbcClient.sql("TRUNCATE career_document").update();
        if (Files.exists(STORAGE_ROOT)) {
            try (var paths = Files.walk(STORAGE_ROOT)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                        .filter(path -> !path.equals(STORAGE_ROOT))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (java.io.IOException exception) {
                                throw new IllegalStateException(exception);
                            }
                        });
            }
        }
        Files.createDirectories(STORAGE_ROOT);
    }

    @Test
    @DisplayName("인증 사용자는 PDF를 업로드하고 자신의 메타데이터와 원본을 조회한다")
    void 인증_사용자는_PDF를_업로드하고_자신의_문서를_조회한다() throws Exception {
        byte[] pdf = pdf();
        MvcResult upload = mockMvc.perform(multipart("/api/career-documents")
                        .file(new MockMultipartFile(
                                "file", "../resume.pdf", "application/pdf", pdf))
                        .with(authentication(authenticationOf(DevelopmentUsers.USER_A)))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalName").value(".._resume.pdf"))
                .andExpect(jsonPath("$.pageCount").value(1))
                .andReturn();
        UUID documentId = UUID.fromString(
                com.jayway.jsonpath.JsonPath.read(
                        upload.getResponse().getContentAsString(), "$.documentId"));

        mockMvc.perform(get("/api/career-documents/{id}", documentId)
                        .with(authentication(authenticationOf(DevelopmentUsers.USER_A))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.storageReference").doesNotExist());
        mockMvc.perform(get("/api/career-documents/{id}/content", documentId)
                        .with(authentication(authenticationOf(DevelopmentUsers.USER_A))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));

        String reference = jdbcClient
                .sql("""
                        SELECT storage_reference
                        FROM career_document
                        WHERE career_document_id = :documentId
                        """)
                .param("documentId", documentId)
                .query(String.class)
                .single();
        assertThat(Files.readAllBytes(STORAGE_ROOT.resolve(reference))).isEqualTo(pdf);
    }

    @Test
    @DisplayName("다른 사용자의 문서와 존재하지 않는 문서는 같은 Not Found로 응답한다")
    void 다른_사용자의_문서는_존재하지_않는_문서처럼_응답한다() throws Exception {
        MvcResult upload = mockMvc.perform(multipart("/api/career-documents")
                        .file(new MockMultipartFile(
                                "file", "resume.pdf", "application/pdf", pdf()))
                        .with(authentication(authenticationOf(DevelopmentUsers.USER_A)))
                        .with(csrf()))
                .andReturn();
        UUID documentId = UUID.fromString(
                com.jayway.jsonpath.JsonPath.read(
                        upload.getResponse().getContentAsString(), "$.documentId"));

        for (UUID requestedId : new UUID[] {documentId, UUID.randomUUID()}) {
            mockMvc.perform(get("/api/career-documents/{id}", requestedId)
                            .with(authentication(authenticationOf(DevelopmentUsers.USER_B))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CAREER_DOCUMENT_NOT_FOUND"));
        }
    }

    @Test
    @DisplayName("비정상 PDF와 CSRF 없는 업로드를 거절하고 저장하지 않는다")
    void 비정상_PDF와_CSRF_없는_업로드를_저장하지_않는다() throws Exception {
        MockMultipartFile invalid =
                new MockMultipartFile("file", "fake.pdf", "application/pdf", "invalid".getBytes());

        mockMvc.perform(multipart("/api/career-documents")
                        .file(invalid)
                        .with(authentication(authenticationOf(DevelopmentUsers.USER_A)))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PDF_SIGNATURE"));
        mockMvc.perform(multipart("/api/career-documents")
                        .file(invalid)
                        .with(authentication(authenticationOf(DevelopmentUsers.USER_A))))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/career-documents")
                        .with(authentication(authenticationOf(DevelopmentUsers.USER_A)))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PDF_EMPTY"));

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM career_document")
                        .query(Integer.class)
                        .single())
                .isZero();
    }

    private static UsernamePasswordAuthenticationToken authenticationOf(CurrentUser user) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                user.userId(), "owner@example.com", "{noop}unused", true);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, principal.passwordHash(), principal.getAuthorities());
    }

    private static byte[] pdf() throws Exception {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }
}
