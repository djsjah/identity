package com.identity.core.image.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.identity.core.image.port.ImageVerificationTaskPublisher;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageVerificationTaskDispatcher {
    private final ImageVerificationTaskService taskService;
    private final ImageVerificationTaskPublisher taskPublisher;

    @Scheduled(fixedDelayString = "${app.verification.dispatch-interval-ms:2000}")
    public void dispatch() {
        taskService.recoverStaleTasks();

        for (String imageId : taskService.findReadyToDispatch()) {
            if (!taskService.reserveDispatch(imageId)) continue;

            try {
                taskPublisher.publish(imageId);
            }
            catch (RuntimeException ex) {
                log.warn("Cannot publish verification task: imageId={}; will retry", imageId, ex);
            }
        }
    }
}
