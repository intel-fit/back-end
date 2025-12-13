package rto.intelfit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rto.intelfit.domain.Subscription;
import rto.intelfit.dto.SubscriptionDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.SubscriptionRepository;
import rto.intelfit.security.CustomUserPrincipal;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubscriptionService {

    private static final List<String> ACTIVE_STATUSES = List.of("trialing", "active", "past_due");

    private final SubscriptionRepository subscriptionRepository;
    private final MembershipService membershipService;

    public SubscriptionDto.SubscriptionInfoResponse getSubscriptionInfo(CustomUserPrincipal userPrincipal) {
        String userId = userPrincipal.getUserId();
        return findLatestSubscription(userId)
                .map(SubscriptionDto.SubscriptionInfoResponse::from)
                .orElseGet(SubscriptionDto.SubscriptionInfoResponse::empty);
    }

    @Transactional
    public SubscriptionDto.SubscriptionCancelResponse cancelSubscription(CustomUserPrincipal userPrincipal) {
        String userId = userPrincipal.getUserId();
        LocalDateTime now = LocalDateTime.now();

        Subscription subscription = subscriptionRepository
                .findTopByUserIdAndStatusInAndExpiredAtAfterOrderByCreatedAtDesc(userId, ACTIVE_STATUSES, now)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "취소할 구독이 없습니다"));

        if ("canceled".equalsIgnoreCase(subscription.getStatus())) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED, "이미 취소된 구독입니다");
        }

        if (subscription.getExpiredAt() != null && subscription.getExpiredAt().isBefore(now)) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_CANCEL_NOT_ALLOWED, "이미 만료된 구독은 취소할 수 없습니다");
        }

        subscription.setStatus("canceled");
        subscription.setCanceledAt(now);
        if (subscription.getExpiredAt() == null || subscription.getExpiredAt().isAfter(now)) {
            subscription.setExpiredAt(now);
        }
        subscriptionRepository.save(subscription);

        membershipService.syncMembership(userId);
        log.info("구독 취소 완료 - userId={}, provider={}, planCode={}", userId, subscription.getProvider(), subscription.getPlanCode());

        return SubscriptionDto.SubscriptionCancelResponse.builder()
                .success(true)
                .message("구독이 취소되었습니다")
                .subscription(SubscriptionDto.SubscriptionInfoResponse.from(subscription))
                .build();
    }

    private java.util.Optional<Subscription> findLatestSubscription(String userId) {
        LocalDateTime now = LocalDateTime.now();
        return subscriptionRepository
                .findTopByUserIdAndStatusInAndExpiredAtAfterOrderByCreatedAtDesc(userId, ACTIVE_STATUSES, now)
                .or(() -> subscriptionRepository.findTopByUserIdOrderByCreatedAtDesc(userId));
    }
}
