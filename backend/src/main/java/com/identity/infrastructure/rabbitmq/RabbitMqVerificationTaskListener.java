package com.identity.infrastructure.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.identity.core.image.service.ImageVerificationProcessor;

@Component
@ConditionalOnProperty(
        prefix = "app.verification",
        name = "worker-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class RabbitMqVerificationTaskListener {
    private final ImageVerificationProcessor verificationProcessor;

    @RabbitListener(
            queues = RabbitMqConfig.VERIFICATION_QUEUE,
            containerFactory = "rabbitListenerFactory"
    )
    public void handle(String imageId) { verificationProcessor.process(imageId); }
}
