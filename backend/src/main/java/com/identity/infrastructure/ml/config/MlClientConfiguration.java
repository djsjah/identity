package com.identity.infrastructure.ml.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
public class MlClientConfiguration {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder templateBuilder, MlProperties properties) {
        return templateBuilder
                .connectTimeout(properties.connectTimeout())
                .readTimeout(properties.readTimeout())
                .build();
    }
}
