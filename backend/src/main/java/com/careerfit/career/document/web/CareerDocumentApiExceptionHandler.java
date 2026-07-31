package com.careerfit.career.document.web;

import com.careerfit.career.document.application.CareerDocumentNotFoundException;
import com.careerfit.career.document.application.InvalidPdfException;
import com.careerfit.career.document.infrastructure.FileStorageException;
import com.careerfit.common.web.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class CareerDocumentApiExceptionHandler {

    @ExceptionHandler(InvalidPdfException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPdf(InvalidPdfException exception) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_PDF_" + exception.failure().name(),
                exception.getMessage());
    }

    @ExceptionHandler(CareerDocumentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            CareerDocumentNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "CAREER_DOCUMENT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleTooLarge(
            MaxUploadSizeExceededException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PDF_TOO_LARGE", "PDF는 10 MiB 이하여야 합니다.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingFile(
            MissingServletRequestPartException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PDF_EMPTY", "업로드 PDF는 필수입니다.");
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageFailure(FileStorageException exception) {
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "FILE_STORAGE_FAILED",
                "파일 저장 처리에 실패했습니다.");
    }

    private static ResponseEntity<ApiErrorResponse> response(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message));
    }
}
