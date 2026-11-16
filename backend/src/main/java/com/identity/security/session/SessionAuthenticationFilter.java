package com.identity.security.session;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.identity.core.auth.session.SessionService;
import com.identity.persistence.user.User;

@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        String sessionToken = getTokenFromCookie(request);
        if (sessionToken != null) {
            sessionService.findUser(sessionToken).ifPresent(SessionAuthenticationFilter::authenticate);
        }

        chain.doFilter(request, response);
    }

    private static void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user,
                null,
                user.getAuthorities()
        ));
    }

    private static String getTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        String sessionToken = null;
        for (Cookie cookie : request.getCookies()) {
            if (SessionCookieService.COOKIE_NAME.equals(cookie.getName())) {
                if (sessionToken != null) return null;
                sessionToken = cookie.getValue();
            }
        }

        return sessionToken;
    }
}
