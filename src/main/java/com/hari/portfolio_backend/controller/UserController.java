
package com.hari.portfolio_backend.controller;

import com.hari.portfolio_backend.entity.User;
import com.hari.portfolio_backend.entity.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;

    public UserController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SecurityContextRepository securityContextRepository) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
    }

    // Register a new user
    @PostMapping
    public ResponseEntity<?> register(@RequestBody User user) {

        if (user.getUsername() == null
                || user.getUsername().isBlank()
                || user.getPassword() == null
                || user.getPassword().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Username and password are required."));
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "Username already exists."));
        }

        // Public registration must never create an administrator.
        user.setRole("USER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message",
                        "Registration successful."));
    }

    // Administrator login
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody User loginUser,
            HttpServletRequest request,
            HttpServletResponse response) {

        System.out.println("LOGIN CONTROLLER REACHED");

        if (loginUser.getUsername() == null
                || loginUser.getPassword() == null) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Username and password are required."));
        }

        User user = userRepository
                .findByUsername(loginUser.getUsername())
                .orElse(null);

        if (user == null
                || !passwordEncoder.matches(
                        loginUser.getPassword(),
                        user.getPassword())) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message",
                            "Invalid username or password."));
        }

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Administrator access required."));
        }

        // Create a session and establish administrator authentication.
        HttpSession session = request.getSession(true);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Persist the authenticated SecurityContext in the HTTP session.
        securityContextRepository.saveContext(
                context, request, response);

        // Used by the existing AdminSessionInterceptor.
        session.setAttribute(
                "ADMIN_USERNAME",
                user.getUsername());

        System.out.println("ADMIN LOGIN SUCCESSFUL");

        return ResponseEntity.ok(Map.of(
                "message", "Login successful.",
                "username", user.getUsername(),
                "role", "ADMIN"
        ));
    }

    // Administrator logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        SecurityContextHolder.clearContext();

        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully."
        ));
    }
}
