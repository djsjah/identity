package com.identity.security.session;

import java.time.Duration;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.server.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.identity.core.auth.session.SessionTicket;
import com.identity.security.config.SecurityProperties;

@Component
@RequiredArgsConstructor
public class SessionCookieService {
    public static final String COOKIE_NAME = "IDENTITY_SESSION";

    private final SecurityProperties properties;

    public void set(HttpServletResponse response, SessionTicket ticket) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = createCookieBuilder(ticket.token());
        if (ticket.rememberMe()) cookieBuilder.maxAge(ticket.ttl());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                createCookieBuilder("")
                        .maxAge(Duration.ZERO)
                        .build()
                        .toString()
        );
    }

    private ResponseCookie.ResponseCookieBuilder createCookieBuilder(String sessionToken) {
        return ResponseCookie
                .from(COOKIE_NAME, sessionToken)
                .httpOnly(true)
                .secure(properties.session().secureCookie())
                .sameSite(Cookie.SameSite.LAX.attributeValue())
                .path("/");
    }
}
