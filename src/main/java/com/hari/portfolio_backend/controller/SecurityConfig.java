
package com.hari.portfolio_backend.controller;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository)
            throws Exception {

        http
            // CSRF disabled for the current fetch-based API.
            .csrf(csrf -> csrf.disable())

            // Store authenticated users in the HTTP session.
            .securityContext(context ->
                context.securityContextRepository(
                    securityContextRepository
                )
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.IF_REQUIRED
                )
            )

            // Login is handled by the existing custom login code.
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            .authorizeHttpRequests(auth -> auth

                // Allow Spring error-page dispatches.
                .dispatcherTypeMatchers(
                    DispatcherType.ERROR
                ).permitAll()

                // Public pages and static resources.
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/portfolio.html",
                    "/login.html",
                    "/admin.html",
                    "/error",
                    "/favicon.ico",
                    "/style.css",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/media/**"
                ).permitAll()

                // Public registration and login endpoints.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/users",
                    "/api/users/login"
                ).permitAll()

                // Public portfolio APIs.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/public/profile",
                    "/api/public/skills",
                    "/api/public/projects"
                ).permitAll()

                // IMPORTANT: Public list of approved portfolio media.
                // This must appear before /api/admin/**.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/admin/media/public"
                ).permitAll()

                // Public file access.
                // The controller must check publicVisible before
                // returning a file.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/admin/media/public/files/**"
                ).permitAll()

                // Only administrators can list all users.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/users"
                ).hasRole("ADMIN")

                // Authenticated users can log out.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/users/logout"
                ).authenticated()

                // All remaining admin APIs require ADMIN.
                .requestMatchers(
                    "/api/admin/**",
                    "/admin/**"
                ).hasRole("ADMIN")

                // Everything else requires authentication.
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
