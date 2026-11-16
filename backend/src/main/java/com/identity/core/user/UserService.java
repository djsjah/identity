package com.identity.core.user;

import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.identity.app.logging.LogExecutionTime;
import com.identity.core.user.dto.ChangeEmailRequestDTO;
import com.identity.core.user.version.UserVersionChangeReason;
import com.identity.core.user.version.UserVersionService;
import com.identity.persistence.user.Role;
import com.identity.persistence.user.User;
import com.identity.persistence.user.UserRepository;

@Service
@LogExecutionTime
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserVersionService userVersionService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User create(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User newUser = userRepository.save(User.builder()
                .username(username)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(password))
                .role(Role.USER)
                .build()
        );

        userVersionService.createInitialVersion(newUser);
        return newUser;
    }

    @Transactional
    public void changeEmail(Long userId, ChangeEmailRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid current password");
        }

        String newEmail = normalizeEmail(request.email());
        if (user.getEmail().equalsIgnoreCase(newEmail)) return;

        if (userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        user.setEmail(newEmail);
        userVersionService.createNextVersion(user, UserVersionChangeReason.EMAIL_CHANGED);
    }

    @Transactional
    public User updatePassword(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(password));
        return user;
    }

    private String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
}
