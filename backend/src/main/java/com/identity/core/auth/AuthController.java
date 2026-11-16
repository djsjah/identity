package com.identity.core.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import com.identity.core.auth.dto.*;
import com.identity.core.auth.service.AuthRateLimiter;
import com.identity.core.auth.service.AuthService;
import com.identity.core.auth.session.SessionService;
import com.identity.persistence.user.User;
import com.identity.security.session.SessionCookieService;
import com.identity.core.auth.session.SessionTicket;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "User authentication API"
)
public class AuthController {
    private final AuthService authService;
    private final AuthRateLimiter authRateLimiter;
    private final SessionService sessionService;
    private final SessionCookieService sessionCookieService;
    private final AuthResponseProperties properties;

    @GetMapping("/csrf")
    @Operation(summary = "Get CSRF token before modifying requests")
    public ResponseEntity<CsrfToken> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(token);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user")
    public ResponseEntity<AuthResponseDTO> me(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new AuthResponseDTO(currentUser.getUsername(), currentUser.getEmail(), currentUser.getRole()));
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Sign in")
    public ResponseEntity<AuthResponseDTO> signIn(
            @Valid @RequestBody SignInRequestDTO request,

            @Parameter(hidden = true)
            @CookieValue(
                    value = SessionCookieService.COOKIE_NAME,
                    required = false
            )
            String previousToken,

            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        authRateLimiter.signIn(httpRequest, request.username());
        return authenticatedResponse(authService.signIn(request, previousToken), httpResponse);
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Sign up")
    public ResponseEntity<AuthResponseDTO> signUp(
            @Valid @RequestBody SignUpRequestDTO request,

            @Parameter(hidden = true)
            @CookieValue(
                    value = SessionCookieService.COOKIE_NAME,
                    required = false
            )
            String previousToken,

            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        authRateLimiter.signUp(httpRequest);
        return authenticatedResponse(authService.signUp(request, previousToken), httpResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out")
    public LogoutResponseDTO logout(
            @Parameter(hidden = true)
            @CookieValue(
                    value = SessionCookieService.COOKIE_NAME,
                    required = false
            )
            String token,
            HttpServletResponse response
    ) {
        sessionService.revoke(token);
        sessionCookieService.clear(response);
        return new LogoutResponseDTO(properties.logoutMessage());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Change password using the current confirmation code")
    public ResponseEntity<AuthResponseDTO> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request,
            @AuthenticationPrincipal User currentUser,
            HttpServletResponse response
    ) {
        authRateLimiter.resetPassword(currentUser.getId());
        return authenticatedResponse(authService.resetPassword(request, currentUser), response);
    }

    private ResponseEntity<AuthResponseDTO> authenticatedResponse(
            SessionTicket sessionTicket,
            HttpServletResponse response
    ) {
        sessionCookieService.set(response, sessionTicket);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new AuthResponseDTO(
                        sessionTicket.user().getUsername(),
                        sessionTicket.user().getEmail(),
                        sessionTicket.user().getRole()
                ));
    }
}
