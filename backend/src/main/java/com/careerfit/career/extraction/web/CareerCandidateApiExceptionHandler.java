package com.careerfit.career.extraction.web;

import com.careerfit.career.extraction.application.CareerCandidateNotFoundException;
import com.careerfit.career.application.CareerExperienceNotFoundException;
import com.careerfit.career.application.CareerVersionAlreadyConfirmedException;
import com.careerfit.common.web.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = {
        CareerCandidateController.class, DocumentCareerConfirmationController.class})
public class CareerCandidateApiExceptionHandler {
    @ExceptionHandler(CareerCandidateNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(CareerCandidateNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "CAREER_CANDIDATE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(CareerExperienceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleExperienceNotFound(
            CareerExperienceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "CAREER_EXPERIENCE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(CareerVersionAlreadyConfirmedException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyConfirmed(
            CareerVersionAlreadyConfirmedException exception) {
        return response(HttpStatus.CONFLICT, "CAREER_VERSION_ALREADY_CONFIRMED", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalid(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_CAREER_CANDIDATE", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(IllegalStateException exception) {
        return response(HttpStatus.CONFLICT, "CAREER_CANDIDATE_NOT_EDITABLE", exception.getMessage());
    }

    private static ResponseEntity<ApiErrorResponse> response(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message));
    }
}
