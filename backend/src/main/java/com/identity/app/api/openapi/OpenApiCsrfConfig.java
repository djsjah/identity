package com.identity.app.api.openapi;

import java.util.EnumSet;
import java.util.Set;

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiCsrfConfig {
    private static final Set<PathItem.HttpMethod> CSRF_METHODS = EnumSet.of(
            PathItem.HttpMethod.POST,
            PathItem.HttpMethod.PUT,
            PathItem.HttpMethod.PATCH,
            PathItem.HttpMethod.DELETE
    );

    @Bean
    public OpenApiCustomizer csrfHeaderCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(path ->
                path.readOperationsMap().forEach((method, operation) -> {
                    if (CSRF_METHODS.contains(method)) {
                        operation.addParametersItem(
                                new HeaderParameter()
                                        .name("X-XSRF-TOKEN")
                                        .required(true)
                                        .schema(new StringSchema())
                        );
                    }
                })
        );
    }
}
