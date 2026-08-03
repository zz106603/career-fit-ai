package com.careerfit.ai.structured.application;

import tools.jackson.databind.JsonNode;

@FunctionalInterface
public interface StructuredOutputDecoder<T> {

    T decode(JsonNode root) throws StructuredOutputValidationException;
}
