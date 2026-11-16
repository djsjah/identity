package com.identity.core.user.version;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.identity.app.utils.UtcDateTimes;
import com.identity.persistence.user.User;
import com.identity.persistence.user.version.UserVersion;
import com.identity.persistence.user.version.UserVersionRepository;

@Service
@RequiredArgsConstructor
public class UserVersionService {
    private final UserVersionRepository userVersionRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void createInitialVersion(User user) {
        userVersionRepository.save(UserVersion.builder()
                .user(user)
                .version(1)
                .role(user.getRole())
                .username(user.getUsername())
                .email(user.getEmail())
                .validFrom(UtcDateTimes.nowMicros())
                .changeReason(UserVersionChangeReason.REGISTRATION.name())
                .build()
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createNextVersion(User user, UserVersionChangeReason reason) {
        UserVersion currentVersion = userVersionRepository.findCurrentByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Current user version not found"));

        LocalDateTime changedAt = UtcDateTimes.nowMicros();

        if (!changedAt.isAfter(currentVersion.getValidFrom())) {
            changedAt = currentVersion.getValidFrom().plus(1, ChronoUnit.MICROS);
        }
        currentVersion.closeAt(changedAt);

        userVersionRepository.flush();
        userVersionRepository.save(UserVersion.builder()
                .user(user)
                .version(currentVersion.getVersion() + 1)
                .role(user.getRole())
                .username(user.getUsername())
                .email(user.getEmail())
                .validFrom(changedAt)
                .changeReason(reason.name())
                .build()
        );
    }
}
