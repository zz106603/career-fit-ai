package com.careerfit.career.document.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("로컬 파일 저장 Adapter 테스트")
class LocalFileStorageAdapterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("저장 참조로 파일을 저장하고 읽고 삭제한다")
    void 저장_참조로_파일을_저장하고_읽고_삭제한다() {
        LocalFileStorageAdapter adapter =
                new LocalFileStorageAdapter(temporaryDirectory.toString());
        byte[] content = "pdf-content".getBytes(StandardCharsets.UTF_8);
        String reference = "career-documents/user/document.pdf";

        adapter.store(reference, content);

        assertThat(adapter.read(reference)).isEqualTo(content);
        adapter.delete(reference);
        assertThat(Files.exists(temporaryDirectory.resolve(reference))).isFalse();
    }

    @Test
    @DisplayName("절대 경로와 저장 루트 이탈을 거절한다")
    void 절대_경로와_저장_루트_이탈을_거절한다() {
        LocalFileStorageAdapter adapter =
                new LocalFileStorageAdapter(temporaryDirectory.toString());

        assertThatThrownBy(() -> adapter.store("../outside.pdf", new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.store(
                        temporaryDirectory.resolve("absolute.pdf").toString(), new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이미 존재하는 저장 키의 원본을 덮어쓰지 않는다")
    void 이미_존재하는_저장_키의_원본을_덮어쓰지_않는다() {
        LocalFileStorageAdapter adapter =
                new LocalFileStorageAdapter(temporaryDirectory.toString());
        String reference = "career-documents/user/document.pdf";
        byte[] original = "original".getBytes(StandardCharsets.UTF_8);
        adapter.store(reference, original);

        assertThatThrownBy(() -> adapter.store(
                        reference, "replacement".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(FileStorageException.class);
        assertThat(adapter.read(reference)).isEqualTo(original);
    }
}
