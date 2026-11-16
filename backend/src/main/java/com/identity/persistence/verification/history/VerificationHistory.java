package com.identity.persistence.verification.history;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;
import lombok.*;

import com.identity.persistence.image.Image;
import com.identity.persistence.user.User;

@Entity
@Table(name = "verification_history")
@NamedEntityGraph(
        name = VerificationHistory.WITH_RELATIONS,
        attributeNodes = { @NamedAttributeNode("image"), @NamedAttributeNode("user") }
)
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationHistory {
    public static final String WITH_RELATIONS = "VerificationHistory.withRelations";

    @Id
    @Column(nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "verification_date", nullable = false)
    private LocalDateTime verificationDate;

    @Column(nullable = false)
    private String result;

    @Builder
    private VerificationHistory(
            String id,
            Image image,
            User user,
            LocalDateTime verificationDate,
            String result
    ) {
        this.id = Objects.requireNonNull(id);
        this.image = Objects.requireNonNull(image);
        this.user = Objects.requireNonNull(user);
        this.verificationDate = Objects.requireNonNull(verificationDate);
        this.result = Objects.requireNonNull(result);
    }
}
