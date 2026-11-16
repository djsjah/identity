package com.identity.persistence.user.version;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.identity.persistence.user.Role;
import com.identity.persistence.user.User;

@Entity
@Table(
        name = "user_version",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_version_user_version",
                columnNames = {"user_id", "version"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "change_reason")
    private String changeReason;

    @Builder
    private UserVersion(
            User user,
            Integer version,
            Role role,
            String username,
            String email,
            LocalDateTime validFrom,
            String changeReason
    ) {
        this.user = Objects.requireNonNull(user);
        this.version = Objects.requireNonNull(version);
        this.role = Objects.requireNonNull(role);
        this.username = Objects.requireNonNull(username);
        this.email = Objects.requireNonNull(email);
        this.validFrom = Objects.requireNonNull(validFrom);
        this.changeReason = changeReason;
    }

    public void closeAt(LocalDateTime validTo) {
        if (this.validTo != null) throw new IllegalStateException("User version is already closed");
        this.validTo = Objects.requireNonNull(validTo);
    }
}
