package com.identity.app.logging;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class LoggingConfiguration {
    @Bean
    public Advisor executionTimeAdvisor() {
        Pointcut pointcut = Pointcuts.union(
                AnnotationMatchingPointcut.forClassAnnotation(LogExecutionTime.class),
                AnnotationMatchingPointcut.forMethodAnnotation(LogExecutionTime.class)
        );

        MethodInterceptor interceptor = invocation -> {
            long start = System.nanoTime();
            Object result = invocation.proceed();
            long executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            log.info(
                    "Class: {}, Method: {} executed in {} ms",
                    Objects.requireNonNull(invocation.getThis()).getClass().getSimpleName(),
                    invocation.getMethod().getName(),
                    executionTime
            );

            return result;
        };

        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }
}
