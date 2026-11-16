package com.identity.security.config;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.server.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.identity.app.api.ApiResponseFactory;
import com.identity.core.auth.session.SessionService;
import com.identity.persistence.user.Role;
import com.identity.persistence.user.UserRepository;
import com.identity.security.session.SessionAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity @EnableMethodSecurity @EnableScheduling
@RequiredArgsConstructor
public class SecurityConfig {
    private final SecurityProperties properties;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;
    private final ApiResponseFactory apiResponseFactory;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepository = new CookieCsrfTokenRepository();

        csrfRepository.setCookieCustomizer(cookie -> cookie
                .httpOnly(true)
                .secure(properties.session().secureCookie())
                .sameSite(Cookie.SameSite.LAX.attributeValue())
                .path("/")
        );

        return http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                (request, response, ex) -> {
                                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                                    objectMapper.writeValue(
                                            response.getWriter(),
                                            apiResponseFactory.error(
                                                    HttpStatus.UNAUTHORIZED,
                                                    "Authentication required"
                                            )
                                    );
                                })
                        .accessDeniedHandler(
                                (request, response, ex) -> {
                                    response.setStatus(HttpStatus.FORBIDDEN.value());
                                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                                    objectMapper.writeValue(
                                            response.getWriter(),
                                            apiResponseFactory.error(HttpStatus.FORBIDDEN, "Access denied")
                                    );
                                })
                )
                .authorizeHttpRequests(request -> request
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/csrf").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/sign-in",
                                "/auth/sign-up",
                                "/auth/logout"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/images/upload").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/users/me/email").authenticated()
                        .requestMatchers("/admin/**").hasAuthority(Role.ADMIN.name())
                        .anyRequest().authenticated()
                )
                .addFilterAfter(
                        new SessionAuthenticationFilter(sessionService),
                        SessionManagementFilter.class
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repository) {
        return username -> repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.cors().allowedOrigins());

        configuration.addAllowedMethod(HttpMethod.GET);
        configuration.addAllowedMethod(HttpMethod.POST);
        configuration.addAllowedMethod(HttpMethod.PUT);
        configuration.addAllowedMethod(HttpMethod.PATCH);
        configuration.addAllowedMethod(HttpMethod.DELETE);
        configuration.addAllowedMethod(HttpMethod.OPTIONS);

        configuration.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE, "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
