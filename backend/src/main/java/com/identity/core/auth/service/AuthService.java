package com.identity.core.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.identity.core.auth.dto.ResetPasswordRequestDTO;
import com.identity.core.auth.dto.SignInRequestDTO;
import com.identity.core.auth.dto.SignUpRequestDTO;
import com.identity.core.auth.session.SessionService;
import com.identity.core.auth.session.SessionTicket;
import com.identity.core.user.UserService;
import com.identity.persistence.user.User;
import com.identity.security.config.SecurityProperties;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final SessionService sessionService;
    private final SecurityProperties properties;

    public SessionTicket signIn(SignInRequestDTO request, String previousToken) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        return sessionService.create(
                Boolean.TRUE.equals(request.rememberMe()),
                (User) authentication.getPrincipal(),
                previousToken
        );
    }

    @Transactional
    public SessionTicket signUp(SignUpRequestDTO request, String previousToken) {
        return sessionService.create(
                false,
                userService.create(request.username(), request.email(), request.password()),
                previousToken
        );
    }

    @Transactional
    public SessionTicket resetPassword(ResetPasswordRequestDTO request, User currentUser) {
        if (!request.username().equals(currentUser.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot reset password");
        }

        boolean correct = MessageDigest.isEqual(
                request.confirmationCode().getBytes(StandardCharsets.UTF_8),
                properties.passwordReset()
                        .confirmationCode()
                        .getBytes(StandardCharsets.UTF_8)
        );

        if (!correct) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid confirmation code");

        User user = userService.updatePassword(currentUser.getUsername(), request.newPassword());
        sessionService.revokeAll(user.getId());
        return sessionService.create(false, user, null);
    }
}
