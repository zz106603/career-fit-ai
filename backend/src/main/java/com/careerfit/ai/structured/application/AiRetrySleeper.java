package com.careerfit.ai.structured.application;

@FunctionalInterface
public interface AiRetrySleeper {

    void sleep(long millis) throws InterruptedException;
}
