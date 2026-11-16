package com.identity.app.api.openapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.identity.app.AppProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiLogger {
    private final AppProperties appProperties;
    private final SwaggerUiConfigProperties swaggerUiProperties;
    private final SpringDocConfigProperties springDocProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info(
                "Swagger UI is available at: {}{}",
                appProperties.publicUrl(),
                swaggerUiProperties.getPath()
        );
        log.info(
                "API documentation is available at: {}{}",
                appProperties.publicUrl(),
                springDocProperties.getApiDocs().getPath()
        );
    }
}
