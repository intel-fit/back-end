package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.Subscription;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findTopByUserIdAndStatusInAndExpiredAtAfterOrderByCreatedAtDesc(String userId, Collection<String> statuses, LocalDateTime now);

    Optional<Subscription> findTopByUserIdAndProviderOrderByCreatedAtDesc(String userId, Subscription.PaymentProvider provider);

    Optional<Subscription> findByProviderSubscriptionId(String subscriptionId);

    java.util.List<Subscription> findByUserId(String userId);

    void deleteAllByUserId(String userId);
}
