package com.careerfit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CareerFitAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerFitAiApplication.class, args);
    }
}
