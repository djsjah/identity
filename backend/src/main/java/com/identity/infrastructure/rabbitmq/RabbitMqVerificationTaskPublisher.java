package com.identity.infrastructure.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.identity.core.image.port.ImageVerificationTaskPublisher;

@Component
@RequiredArgsConstructor
public class RabbitMqVerificationTaskPublisher implements ImageVerificationTaskPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(String imageId) { rabbitTemplate.convertAndSend(RabbitMqConfig.VERIFICATION_QUEUE, imageId); }
}
