package com.careerfit.career.document.infrastructure;

import com.careerfit.career.document.application.FileStoragePort;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private final Path root;

    public LocalFileStorageAdapter(
            @Value("${career-fit.storage.local.root}") String configuredRoot) {
        root = Path.of(configuredRoot).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new FileStorageException("파일 저장 루트를 준비할 수 없습니다.", exception);
        }
    }

    @Override
    public void store(String storageReference, byte[] content) {
        Path target = resolve(storageReference);
        Path temporary = null;
        try {
            Files.createDirectories(target.getParent());
            if (Files.exists(target)) {
                throw new FileStorageException(
                        "이미 존재하는 저장 참조는 덮어쓸 수 없습니다.",
                        new java.nio.file.FileAlreadyExistsException(storageReference));
            }
            temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
            Files.write(temporary, content);
            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, target);
            }
        } catch (IOException exception) {
            deleteTemporary(temporary);
            throw new FileStorageException("파일을 저장할 수 없습니다.", exception);
        }
    }

    @Override
    public byte[] read(String storageReference) {
        try {
            return Files.readAllBytes(resolve(storageReference));
        } catch (IOException exception) {
            throw new FileStorageException("파일을 읽을 수 없습니다.", exception);
        }
    }

    @Override
    public void delete(String storageReference) {
        try {
            Files.deleteIfExists(resolve(storageReference));
        } catch (IOException exception) {
            throw new FileStorageException("파일을 삭제할 수 없습니다.", exception);
        }
    }

    private Path resolve(String storageReference) {
        if (storageReference == null || storageReference.isBlank()) {
            throw new IllegalArgumentException("저장 참조는 필수입니다.");
        }
        Path reference = Path.of(storageReference);
        if (reference.isAbsolute()) {
            throw new IllegalArgumentException("절대 저장 경로는 허용하지 않습니다.");
        }
        Path resolved = root.resolve(reference).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("저장 루트를 벗어날 수 없습니다.");
        }
        return resolved;
    }

    private static void deleteTemporary(Path temporary) {
        if (temporary == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
            // 최초 저장 실패를 보존한다.
        }
    }
}
