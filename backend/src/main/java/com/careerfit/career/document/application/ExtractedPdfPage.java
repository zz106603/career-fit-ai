package com.careerfit.career.document.application;

public record ExtractedPdfPage(int pageNumber, String text, String checksumSha256) {}
