package com.sayan.rankly.services;

import com.sayan.rankly.dto.auth.AuthUserResponse;
import com.sayan.rankly.dto.user.UpdatePreferencesRequest;
import com.sayan.rankly.dto.user.UserQuotaResponse;
import com.sayan.rankly.entities.User;
import com.sayan.rankly.repositories.UserRepository;
import com.sayan.rankly.repositories.SearchBatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SearchBatchRepository searchBatchRepository;

    public UserService(UserRepository userRepository, SearchBatchRepository searchBatchRepository) {
        this.userRepository = userRepository;
        this.searchBatchRepository = searchBatchRepository;
    }

    @Transactional
    public AuthUserResponse updatePreferences(Authentication authentication, UpdatePreferencesRequest request) {
        User user = getAuthenticatedUser(authentication);

        if (request.themePreference() != null) {
            user.setThemePreference(request.themePreference().trim().toUpperCase());
        }
        if (request.emailAlertsEnabled() != null) {
            user.setEmailAlertsEnabled(request.emailAlertsEnabled());
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName().trim());
        }

        User saved = userRepository.save(user);

        return new AuthUserResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getThemePreference(),
                saved.isEmailAlertsEnabled(),
                saved.getDailyApiQuota()
        );
    }

    @Transactional(readOnly = true)
    public UserQuotaResponse getDailyQuota(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        int dailyLimit = user.getDailyApiQuota() != null ? user.getDailyApiQuota() : 50;
        // Once SearchBatchRepository is added, replace 0 with today's completed/in-progress keyword checks for user.getId()
//        int usedToday = 0;
        OffsetDateTime startOfDay = OffsetDateTime.now().toLocalDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        int usedToday = searchBatchRepository.sumCompletedKeywordsSince(user.getId(), startOfDay);
        int remainingToday = Math.max(0, dailyLimit - usedToday);

        return new UserQuotaResponse(dailyLimit, usedToday, remainingToday);
    }

//    Utility Methods
    public User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String email = authentication.getName().trim().toLowerCase();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
