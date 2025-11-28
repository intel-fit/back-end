package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.User;
import rto.intelfit.repository.SubscriptionRepository;
import rto.intelfit.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {

    private static final List<String> ACTIVE_STATUSES = List.of("trialing", "active", "past_due");

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public void markPremium(String userId, String reason) {
        userRepository.findByUserId(userId).ifPresent(user -> {
            if (user.getMembershipType() != User.MembershipType.PREMIUM) {
                log.info("Upgrading user {} to PREMIUM (reason: {})", userId, reason);
                user.setMembershipType(User.MembershipType.PREMIUM);
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public void syncMembership(String userId) {
        LocalDateTime now = LocalDateTime.now();

        // 만료 처리
        subscriptionRepository.findByUserId(userId).stream()
                .filter(sub -> sub.getExpiredAt() != null && sub.getExpiredAt().isBefore(now))
                .filter(sub -> ACTIVE_STATUSES.contains(sub.getStatus()))
                .forEach(sub -> {
                    sub.setStatus("expired");
                    subscriptionRepository.save(sub);
                });

        boolean hasActiveSubscription = subscriptionRepository
                .findTopByUserIdAndStatusInAndExpiredAtAfterOrderByCreatedAtDesc(userId, ACTIVE_STATUSES, now)
                .isPresent();

        userRepository.findByUserId(userId).ifPresent(user -> {
            if (hasActiveSubscription) {
                if (user.getMembershipType() != User.MembershipType.PREMIUM) {
                    log.info("Sync: upgrading user {} to PREMIUM (active subscription exists)", userId);
                    user.setMembershipType(User.MembershipType.PREMIUM);
                    userRepository.save(user);
                }
            } else if (user.getMembershipType() == User.MembershipType.PREMIUM) {
                log.info("Sync: downgrading user {} to FREE (no active subscriptions)", userId);
                user.setMembershipType(User.MembershipType.FREE);
                userRepository.save(user);
            }
        });
    }
}
