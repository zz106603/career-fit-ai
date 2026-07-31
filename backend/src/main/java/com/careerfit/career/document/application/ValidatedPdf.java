package com.careerfit.career.document.application;

public record ValidatedPdf(int pageCount, String checksumSha256) {}
