package com.careerfit.ai.structured.application;

import org.springframework.stereotype.Component;

@Component
public class ThreadAiRetrySleeper implements AiRetrySleeper {

    @Override
    public void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
