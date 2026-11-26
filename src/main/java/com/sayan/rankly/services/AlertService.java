package com.sayan.rankly.services;

import com.sayan.rankly.dto.alert.AlertResponse;
import com.sayan.rankly.dto.alert.UnreadCountResponse;
import com.sayan.rankly.entities.Alert;
import com.sayan.rankly.entities.Domain;
import com.sayan.rankly.entities.Keyword;
import com.sayan.rankly.entities.User;
import com.sayan.rankly.repositories.AlertRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserService userService;
    private final DomainService domainService;

    public AlertService(
            AlertRepository alertRepository,
            UserService userService,
            DomainService domainService
    ) {
        this.alertRepository = alertRepository;
        this.userService = userService;
        this.domainService = domainService;
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts(
            Authentication authentication,
            Long domainId,
            boolean unreadOnly
    ) {
        User user = userService.getAuthenticatedUser(authentication);
        if (domainId != null) {
            domainService.getOwnedDomain(authentication, domainId);
        }

        return alertRepository.findFilteredAlerts(user.getId(), domainId, unreadOnly)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        long count = alertRepository.countByUserIdAndIsReadFalse(user.getId());
        return new UnreadCountResponse(count);
    }

    @Transactional
    public AlertResponse markAsRead(Authentication authentication, Long alertId) {
        User user = userService.getAuthenticatedUser(authentication);
        Alert alert = alertRepository.findByIdAndUserId(alertId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));

        if (!alert.isRead()) {
            alert.setRead(true);
            alert = alertRepository.save(alert);
        }

        return toResponse(alert);
    }

    @Transactional
    public int markAllAsRead(Authentication authentication, Long domainId) {
        User user = userService.getAuthenticatedUser(authentication);
        if (domainId != null) {
            domainService.getOwnedDomain(authentication, domainId);
        }
        return alertRepository.markAllAsRead(user.getId(), domainId);
    }

    /**
     * Called by the rank tracker / 6:00 AM scheduled monitor when a keyword rank changes,
     * especially when rank goes above 100 or drops out of top results.
     */
    @Transactional
    public void recordRankChangeAlert(
            User user,
            Domain domain,
            Keyword keyword,
            Integer oldRank,
            Integer newRank
    ) {
        boolean isAbove100OrUnranked = (newRank == null || newRank > 100);
        boolean wasWithin100 = (oldRank != null && oldRank >= 1 && oldRank <= 100);

        String alertType;
        String message;

        if (isAbove100OrUnranked && wasWithin100) {
            alertType = "RANK_ABOVE_100";
            message = String.format(
                    "Keyword \"%s\" on %s moved from rank #%d to %s (outside Top 100).",
                    keyword.getPhrase(),
                    domain.getDomainName(),
                    oldRank,
                    newRank == null ? "unranked" : "#" + newRank
            );
        } else if (isAbove100OrUnranked && oldRank == null) {
            alertType = "RANK_ABOVE_100";
            message = String.format(
                    "Keyword \"%s\" on %s is currently %s (outside Top 100).",
                    keyword.getPhrase(),
                    domain.getDomainName(),
                    newRank == null ? "unranked" : "#" + newRank
            );
        } else if (oldRank != null && newRank != null && newRank > oldRank) {
            alertType = "RANK_DROP";
            message = String.format(
                    "Keyword \"%s\" on %s dropped from rank #%d to #%d.",
                    keyword.getPhrase(),
                    domain.getDomainName(),
                    oldRank,
                    newRank
            );
        } else if (oldRank != null && newRank != null && newRank < oldRank) {
            alertType = "RANK_GAIN";
            message = String.format(
                    "Keyword \"%s\" on %s improved from rank #%d to #%d.",
                    keyword.getPhrase(),
                    domain.getDomainName(),
                    oldRank,
                    newRank
            );
        } else {
            return;
        }

        Alert alert = new Alert();
        alert.setUser(user);
        alert.setDomain(domain);
        alert.setKeyword(keyword);
        alert.setAlertType(alertType);
        alert.setOldRank(oldRank);
        alert.setNewRank(newRank);
        alert.setMessage(message);
        alert.setRead(false);
        alert.setEmailSent(false);

        alertRepository.save(alert);
    }

    private AlertResponse toResponse(Alert alert) {
        Integer newRank = alert.getNewRank();
        boolean outOfTop100 = (newRank == null || newRank > 100);

        return new AlertResponse(
                alert.getId(),
                alert.getDomain().getId(),
                alert.getDomain().getDomainName(),
                alert.getKeyword().getId(),
                alert.getKeyword().getPhrase(),
                alert.getAlertType(),
                alert.getOldRank(),
                newRank,
                outOfTop100,
                alert.getMessage(),
                alert.isRead(),
                alert.getCreatedAt()
        );
    }

//     to record alerts from the manual/async and the cron runs
    @Transactional
    public void recordRankChangeAlert(
            Domain domain,
            Keyword keyword,
            Integer oldRank,
            Integer newRank,
            int significantMoveThreshold
    ) {
        if (domain == null || keyword == null || domain.getUser() == null) {
            return;
        }

        String alertType = null;
        String message = null;

        if (newRank == null) {
            // Rank > 100 (unranked / outside top 100)
            if (oldRank != null) {
                alertType = "DROPPED_OUT";
                message = String.format(
                        "\"%s\" on %s dropped out of the Top 100 (was #%d, now > 100).",
                        keyword.getPhrase(), domain.getDomainName(), oldRank
                );
            } else {
                alertType = "RANK_ABOVE_100";
                message = String.format(
                        "\"%s\" on %s is ranking outside the Top 100 (> 100).",
                        keyword.getPhrase(), domain.getDomainName()
                );
            }
        } else if (oldRank != null) {
            int delta = oldRank - newRank; // positive = improved (e.g. 12 -> 4 => +8)
            if (delta <= -significantMoveThreshold) {
                alertType = "RANK_DROP";
                message = String.format(
                        "\"%s\" on %s dropped from #%d to #%d (%d positions).",
                        keyword.getPhrase(), domain.getDomainName(), oldRank, newRank, delta
                );
            } else if (delta >= significantMoveThreshold) {
                alertType = "RANK_GAIN";
                message = String.format(
                        "\"%s\" on %s improved from #%d to #%d (+%d positions).",
                        keyword.getPhrase(), domain.getDomainName(), oldRank, newRank, delta
                );
            }
        }

        if (alertType == null) {
            return;
        }

        Alert alert = new Alert();
        alert.setUser(domain.getUser());
        alert.setDomain(domain);
        alert.setKeyword(keyword);
        alert.setAlertType(alertType);
        alert.setOldRank(oldRank);
        alert.setNewRank(newRank);
        alert.setMessage(message);
        alert.setRead(false);
        alert.setEmailSent(false);

        alertRepository.save(alert);
    }
}
