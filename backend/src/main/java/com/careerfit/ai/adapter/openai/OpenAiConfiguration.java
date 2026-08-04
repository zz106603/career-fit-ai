package com.careerfit.ai.adapter.openai;

import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "career-fit.ai.provider", havingValue = "openai")
@EnableConfigurationProperties(OpenAiProperties.class)
class OpenAiConfiguration {

    @Bean
    RestClient openAiRestClient(OpenAiProperties properties) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(properties.timeout());
        return RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .requestFactory(factory)
                .build();
    }
}
