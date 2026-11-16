package com.identity.core.auth.session;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionCleanupJob {
    private final SessionService sessionService;

    @Scheduled(fixedDelayString = "${app.security.session.cleanup-interval}")
    public void cleanup() { sessionService.deleteExpired(); }
}
