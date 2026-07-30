package com.careerfit.identity.auth.web;

public record CsrfTokenResponse(String headerName, String parameterName, String token) {}
