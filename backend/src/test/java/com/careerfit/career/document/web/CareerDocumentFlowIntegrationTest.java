package com.careerfit.career.document.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.common.async.application.JobExecutionService;
import com.careerfit.common.async.application.JobWorker;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionStatus;
import com.careerfit.identity.CurrentUser;
import com.careerfit.identity.development.DevelopmentUsers;
import com.careerfit.identity.security.AuthenticatedUserPrincipal;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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

    @Autowired
    private JobExecutionService jobExecutionService;

    @Autowired
    private JobWorker jobWorker;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("career-fit.storage.local.root", STORAGE_ROOT::toString);
    }

    @BeforeEach
    void 문서_데이터와_파일을_초기화한다() throws Exception {
        jdbcClient.sql("TRUNCATE job_execution, career_document CASCADE").update();
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

    @Test
    @DisplayName("추출 요청을 중복 방지하고 작업 실행 후 페이지 텍스트를 저장한다")
    void 추출_요청을_실행해_페이지_텍스트를_저장한다() throws Exception {
        UUID documentId = uploadPdf(pdf("first", null, "third"), DevelopmentUsers.USER_A);

        MvcResult first = requestExtraction(documentId, DevelopmentUsers.USER_A)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andReturn();
        MvcResult duplicate = requestExtraction(documentId, DevelopmentUsers.USER_A)
                .andExpect(status().isAccepted())
                .andReturn();
        UUID analysisId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                first.getResponse().getContentAsString(), "$.analysisId"));
        assertThat(com.jayway.jsonpath.JsonPath.<String>read(
                duplicate.getResponse().getContentAsString(), "$.analysisId"))
                .isEqualTo(analysisId.toString());

        JobExecution queued = jobExecutionService.findQueued(10).getFirst();
        assertThat(jobWorker.execute(queued)).isTrue();
        JobExecution candidateExtraction = jobExecutionService.findQueued(10).getFirst();
        assertThat(jobWorker.execute(candidateExtraction)).isTrue();

        JobExecution completed = jobExecutionService.find(queued.userId(), queued.id());
        assertThat(completed.status()).isEqualTo(JobExecutionStatus.SUCCEEDED);
        assertThat(jdbcClient.sql("SELECT page_text FROM career_document_page "
                        + "WHERE document_analysis_id = :id ORDER BY page_number")
                .param("id", analysisId).query(String.class).list())
                .hasSize(3)
                .satisfies(texts -> {
                    assertThat(texts.get(0)).contains("first");
                    assertThat(texts.get(1)).isEmpty();
                    assertThat(texts.get(2)).contains("third");
                });
        assertThat(jdbcClient.sql("SELECT status FROM career_document_analysis "
                        + "WHERE document_analysis_id = :id")
                .param("id", analysisId).query(String.class).single())
                .isEqualTo("SUCCEEDED");
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM career_extraction_candidate "
                        + "WHERE document_analysis_id = :id AND status = 'PENDING_REVIEW'")
                .param("id", analysisId).query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbcClient.sql("SELECT excerpt FROM experience_evidence "
                        + "WHERE document_analysis_id = :id")
                .param("id", analysisId).query(String.class).single()).contains("first");
    }

    @Test
    @DisplayName("다른 사용자는 문서 텍스트 추출을 요청할 수 없다")
    void 다른_사용자는_텍스트_추출을_요청할_수_없다() throws Exception {
        UUID documentId = uploadPdf(pdf("owner"), DevelopmentUsers.USER_A);

        requestExtraction(documentId, DevelopmentUsers.USER_B)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAREER_DOCUMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("텍스트 추출 실패 문서에 대체 텍스트 Snapshot과 새 분석을 생성한다")
    void 추출_실패_문서에_대체_텍스트를_저장한다() throws Exception {
        UUID documentId = uploadPdf(pdf(), DevelopmentUsers.USER_A);
        requestExtraction(documentId, DevelopmentUsers.USER_A).andExpect(status().isAccepted());
        JobExecution extraction = jobExecutionService.findQueued(10).getFirst();
        assertThat(jobWorker.execute(extraction)).isTrue();
        assertThat(jobExecutionService.find(extraction.userId(), extraction.id()).failureCode())
                .isEqualTo("PDF_TEXT_EMPTY");

        MvcResult created = requestAlternativeText(
                        documentId, DevelopmentUsers.USER_A, "  first\r\nsecond  ", true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inputKind").value("PASTED_TEXT"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.textLength").value(16))
                .andExpect(jsonPath("$.text").doesNotExist())
                .andReturn();
        UUID analysisId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.documentAnalysisId"));

        requestExtraction(documentId, DevelopmentUsers.USER_A)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.analysisId").value(analysisId.toString()))
                .andExpect(jsonPath("$.jobExecutionId").doesNotExist());

        requestAlternativeText(documentId, DevelopmentUsers.USER_A, "  first\nsecond  ", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentAnalysisId").value(analysisId.toString()));

        assertThat(jdbcClient.sql("SELECT alternative_text FROM career_document_alternative_text "
                        + "WHERE document_analysis_id = :id")
                .param("id", analysisId).query(String.class).single())
                .isEqualTo("  first\nsecond  ");
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM career_document_alternative_text")
                .query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM career_document_analysis "
                        + "WHERE input_kind = 'PDF_TEXT' AND status = 'FAILED' "
                        + "AND failure_code = 'PDF_TEXT_EMPTY'")
                .query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbcClient.sql("SELECT job_execution_id FROM career_document_analysis "
                        + "WHERE document_analysis_id = :id")
                .param("id", analysisId).query(UUID.class).optional()).isEmpty();
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM job_execution")
                .query(Integer.class).single()).isEqualTo(2);
        JobExecution candidateExtraction = jobExecutionService.findQueued(10).getFirst();
        assertThat(jobWorker.execute(candidateExtraction)).isTrue();
        assertThat(jdbcClient.sql("SELECT status FROM career_document_analysis "
                        + "WHERE document_analysis_id = :id")
                .param("id", analysisId).query(String.class).single()).isEqualTo("SUCCEEDED");
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM career_extraction_candidate "
                        + "WHERE document_analysis_id = :id")
                .param("id", analysisId).query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    @DisplayName("추출 성공 문서와 다른 사용자의 문서는 대체 텍스트 입력을 거부한다")
    void 허용되지_않은_문서의_대체_텍스트를_거부한다() throws Exception {
        UUID documentId = uploadPdf(pdf("success"), DevelopmentUsers.USER_A);
        requestExtraction(documentId, DevelopmentUsers.USER_A).andExpect(status().isAccepted());
        assertThat(jobWorker.execute(jobExecutionService.findQueued(10).getFirst())).isTrue();

        requestAlternativeText(documentId, DevelopmentUsers.USER_A, "fallback", true)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALTERNATIVE_TEXT_NOT_ALLOWED"));
        requestAlternativeText(documentId, DevelopmentUsers.USER_B, "fallback", true)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAREER_DOCUMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("빈 대체 텍스트와 CSRF 없는 요청을 거부한다")
    void 빈_대체_텍스트와_CSRF_없는_요청을_거부한다() throws Exception {
        UUID documentId = uploadPdf(pdf(), DevelopmentUsers.USER_A);

        requestAlternativeText(documentId, DevelopmentUsers.USER_A, " \r\n ", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ALTERNATIVE_TEXT_EMPTY"));
        requestAlternativeText(documentId, DevelopmentUsers.USER_A, "fallback", false)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("문서 분석 이력을 최신순으로 조회하고 상태별 다음 행동을 제공한다")
    void 문서_분석_이력과_다음_행동을_조회한다() throws Exception {
        UUID documentId = uploadPdf(pdf("career"), DevelopmentUsers.USER_A);
        requestExtraction(documentId, DevelopmentUsers.USER_A).andExpect(status().isAccepted());
        JobExecution extraction = jobExecutionService.findQueued(10).getFirst();
        assertThat(jobWorker.execute(extraction)).isTrue();
        assertThat(jobWorker.execute(jobExecutionService.findQueued(10).getFirst())).isTrue();

        mockMvc.perform(get("/api/career-documents/{id}/analyses", documentId)
                        .with(authentication(authenticationOf(DevelopmentUsers.USER_A))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(documentId.toString()))
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$[0].nextAction").value("REVIEW_CANDIDATES"))
                .andExpect(jsonPath("$[0].progress").doesNotExist())
                .andExpect(jsonPath("$[0].estimatedCompletionAt").doesNotExist());

        mockMvc.perform(get("/api/career-documents/{id}/analyses", documentId)
                        .with(authentication(authenticationOf(DevelopmentUsers.USER_B))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAREER_DOCUMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("PDF 전체 재실행은 과거 분석을 보존하고 새 활성 실행 하나만 만든다")
    void PDF_전체_재실행은_새_분석을_하나만_만든다() throws Exception {
        UUID documentId = uploadPdf(pdf("career"), DevelopmentUsers.USER_A);
        requestExtraction(documentId, DevelopmentUsers.USER_A).andExpect(status().isAccepted());
        assertThat(jobWorker.execute(jobExecutionService.findQueued(10).getFirst())).isTrue();
        assertThat(jobWorker.execute(jobExecutionService.findQueued(10).getFirst())).isTrue();

        MvcResult first = requestRerun(documentId, DevelopmentUsers.USER_A)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.inputKind").value("PDF_TEXT"))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.nextAction").value("WAIT"))
                .andReturn();
        String rerunAnalysisId = com.jayway.jsonpath.JsonPath.read(
                first.getResponse().getContentAsString(), "$.documentAnalysisId");
        requestRerun(documentId, DevelopmentUsers.USER_A)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.documentAnalysisId").value(rerunAnalysisId));

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM career_document_analysis "
                        + "WHERE document_id = :documentId")
                .param("documentId", documentId).query(Integer.class).single()).isEqualTo(2);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM career_document_analysis "
                        + "WHERE document_id = :documentId AND status IN ('QUEUED', 'PROCESSING')")
                .param("documentId", documentId).query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM career_extraction_candidate "
                        + "WHERE document_analysis_id <> :rerunAnalysisId")
                .param("rerunAnalysisId", UUID.fromString(rerunAnalysisId))
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    @DisplayName("대체 텍스트 전체 재실행은 최신 Snapshot을 복사하고 PDF 추출을 생략한다")
    void 대체_텍스트_전체_재실행은_Snapshot을_복사한다() throws Exception {
        UUID documentId = uploadPdf(pdf(), DevelopmentUsers.USER_A);
        requestExtraction(documentId, DevelopmentUsers.USER_A).andExpect(status().isAccepted());
        assertThat(jobWorker.execute(jobExecutionService.findQueued(10).getFirst())).isTrue();
        requestAlternativeText(documentId, DevelopmentUsers.USER_A, "saved career", true)
                .andExpect(status().isCreated());
        assertThat(jobWorker.execute(jobExecutionService.findQueued(10).getFirst())).isTrue();

        MvcResult rerun = requestRerun(documentId, DevelopmentUsers.USER_A)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.inputKind").value("PASTED_TEXT"))
                .andExpect(jsonPath("$.jobExecutionId").doesNotExist())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn();
        UUID analysisId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                rerun.getResponse().getContentAsString(), "$.documentAnalysisId"));

        assertThat(jdbcClient.sql("SELECT alternative_text FROM career_document_alternative_text "
                        + "WHERE document_analysis_id = :analysisId")
                .param("analysisId", analysisId).query(String.class).single())
                .isEqualTo("saved career");
        assertThat(jobExecutionService.findQueued(10))
                .singleElement()
                .satisfies(job -> assertThat(job.type().name())
                        .isEqualTo("CAREER_CANDIDATE_EXTRACTION"));
    }

    private org.springframework.test.web.servlet.ResultActions requestExtraction(
            UUID documentId, CurrentUser user) throws Exception {
        return mockMvc.perform(post("/api/career-documents/{id}/extractions", documentId)
                .with(authentication(authenticationOf(user)))
                .with(csrf()));
    }

    private org.springframework.test.web.servlet.ResultActions requestRerun(
            UUID documentId, CurrentUser user) throws Exception {
        return mockMvc.perform(post("/api/career-documents/{id}/analyses/reruns", documentId)
                .with(authentication(authenticationOf(user)))
                .with(csrf()));
    }

    private org.springframework.test.web.servlet.ResultActions requestAlternativeText(
            UUID documentId, CurrentUser user, String text, boolean withCsrf) throws Exception {
        var request = post("/api/career-documents/{id}/alternative-texts", documentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"" + jsonEscape(text) + "\"}")
                .with(authentication(authenticationOf(user)));
        if (withCsrf) {
            request.with(csrf());
        }
        return mockMvc.perform(request);
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private UUID uploadPdf(byte[] content, CurrentUser user) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/career-documents")
                        .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", content))
                        .with(authentication(authenticationOf(user)))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.documentId"));
    }

    private static UsernamePasswordAuthenticationToken authenticationOf(CurrentUser user) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                user.userId(), "owner@example.com", "{noop}unused", true);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, principal.passwordHash(), principal.getAuthorities());
    }

    private static byte[] pdf() throws Exception {
        return pdf((String) null);
    }

    private static byte[] pdf(String... texts) throws Exception {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String text : texts) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (text != null) {
                    try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        stream.newLineAtOffset(50, 700);
                        stream.showText(text);
                        stream.endText();
                    }
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
