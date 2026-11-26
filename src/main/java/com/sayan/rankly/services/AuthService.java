package com.sayan.rankly.services;

import com.sayan.rankly.dto.auth.AuthUserResponse;
import com.sayan.rankly.dto.auth.LoginRequest;
import com.sayan.rankly.dto.auth.RegisterRequest;
import com.sayan.rankly.entities.User;
import com.sayan.rankly.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final int DEFAULT_DAILY_QUOTA = 50;
    private final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthUserResponse registerAndLogin(
            RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            LOGGER.info(normalizedEmail + " is already registered");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName() != null ? request.fullName().trim() : null);
        user.setThemePreference("SYSTEM");
        user.setEmailAlertsEnabled(true);
        user.setDailyApiQuota(DEFAULT_DAILY_QUOTA);

        User savedUser = userRepository.save(user);

        authenticateAndSaveContext(
                normalizedEmail,
                request.password(),
                httpRequest,
                httpResponse
        );

        return toResponse(savedUser);
    }

    public AuthUserResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String normalizedEmail = request.email().trim().toLowerCase();

        authenticateAndSaveContext(
                normalizedEmail,
                request.password(),
                httpRequest,
                httpResponse
        );

        User user = findByEmailOrThrow(normalizedEmail);
        return toResponse(user);
    }

    public void logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Authentication authentication
    ) {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(httpRequest, httpResponse, authentication);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getCurrentUser(Authentication authentication) {
        String email = authentication.getName().trim().toLowerCase();
        User user = findByEmailOrThrow(email);
        return toResponse(user);
    }


//    UTILITY METHODS
    private void authenticateAndSaveContext(
            String email,
            String rawPassword,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        Authentication authToken = UsernamePasswordAuthenticationToken.unauthenticated(email, rawPassword);
        Authentication authResult = authenticationManager.authenticate(authToken);

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authResult);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }

    private User findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AuthUserResponse toResponse(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getThemePreference(),
                user.isEmailAlertsEnabled(),
                user.getDailyApiQuota()
        );
    }
}
