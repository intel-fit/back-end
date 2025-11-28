package rto.intelfit.service;

import com.stripe.model.Invoice;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import rto.intelfit.domain.PaymentHistory;
import rto.intelfit.domain.Subscription;
import rto.intelfit.domain.User;
import rto.intelfit.dto.StripeCheckoutResponse;
import rto.intelfit.dto.SubscriptionStatusDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.repository.PaymentHistoryRepository;
import rto.intelfit.repository.SubscriptionRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.MembershipService;
import rto.intelfit.util.JwtUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StripeService {

    private static final List<String> ACTIVE_STATUSES = List.of("trialing", "active", "past_due");
    private static final String PLAN_MONTHLY = "PREMIUM_MONTHLY";
    private static final String PLAN_ANNUAL = "PREMIUM_ANNUAL";

    @Value("${stripe.price-id.monthly}")
    private String stripePriceIdMonthly;

    @Value("${stripe.price-id.annual}")
    private String stripePriceIdAnnual;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final JwtUtil jwtUtil;

    public StripeCheckoutResponse createCheckoutSession(String planCode) throws Exception {
        User user = getAuthenticatedUser();

        PlanInfo planInfo = resolvePlan(planCode);

        String existingCustomerId = subscriptionRepository.findTopByUserIdAndProviderOrderByCreatedAtDesc(
                        user.getUserId(), Subscription.PaymentProvider.STRIPE)
                .map(Subscription::getProviderCustomerId)
                .orElse(null);

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(user.getUserId())
                .putMetadata("user_id", user.getUserId())
                .putMetadata("plan_code", planInfo.planCode())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(planInfo.priceId())
                                .setQuantity(1L)
                                .build()
                )
                .setSubscriptionData(
                        SessionCreateParams.SubscriptionData.builder()
                                .putMetadata("user_id", user.getUserId())
                                .putMetadata("plan_code", planInfo.planCode())
                                .build()
                );

        if (StringUtils.hasText(existingCustomerId)) {
            params.setCustomer(existingCustomerId);
        } else if (StringUtils.hasText(user.getEmail())) {
            params.setCustomerEmail(user.getEmail());
        }

        Session session = Session.create(params.build());
        return StripeCheckoutResponse.builder()
                .sessionId(session.getId())
                .url(session.getUrl())
                .build();
    }

    public SubscriptionStatusDto getSubscriptionStatusForCurrentUser() {
        User user = getAuthenticatedUser();
        return subscriptionRepository
                .findTopByUserIdAndStatusInAndExpiredAtAfterOrderByCreatedAtDesc(user.getUserId(), ACTIVE_STATUSES, LocalDateTime.now())
                .map(sub -> SubscriptionStatusDto.builder()
                        .hasActiveSubscription(true)
                        .status(sub.getStatus())
                        .currentPeriodEnd(sub.getExpiredAt())
                        .provider(sub.getProvider().name())
                        .providerSubscriptionId(sub.getProviderSubscriptionId())
                        .build())
                .orElse(
                        SubscriptionStatusDto.builder()
                                .hasActiveSubscription(false)
                                .build()
                );
    }

    @Transactional
    public void processCheckoutSuccess(String sessionId) throws Exception {
        Session session = Session.retrieve(sessionId);
        handleCheckoutCompleted(session);

        String subscriptionId = session.getSubscription();
        if (StringUtils.hasText(subscriptionId)) {
            com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(subscriptionId);
            String userId = resolveUserId(stripeSub);

            Invoice invoice = null;
            if (stripeSub.getLatestInvoiceObject() instanceof Invoice inv) {
                invoice = inv;
            } else if (stripeSub.getLatestInvoice() != null) {
                invoice = Invoice.retrieve(stripeSub.getLatestInvoice());
            }

            if (invoice != null) {
                handleInvoicePaid(invoice);
            } else if (StringUtils.hasText(userId)) {
                // fallback: record as approved without invoice data
                PlanInfo planInfo = resolvePlan(resolvePlanCode(stripeSub));
                recordStripePayment(userId, invoiceLikeOrderId(sessionId, stripeSub.getId()), stripeSub.getCustomer(), amountFromSubscription(stripeSub), planInfo.planCode(), PaymentHistory.PaymentStatus.APPROVED);
                membershipService.syncMembership(userId);
            }
        }
    }

    @Transactional
    public void handleCheckoutCompleted(Session session) throws Exception {
        String subscriptionId = session.getSubscription();
        if (!StringUtils.hasText(subscriptionId)) {
            log.warn("Checkout completed event without subscription id, session={}", session.getId());
            return;
        }

        String userId = Optional.ofNullable(session.getClientReferenceId())
                .orElseGet(() -> Optional.ofNullable(session.getMetadata()).map(m -> m.get("user_id")).orElse(null));

        com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(subscriptionId);

        if (!StringUtils.hasText(userId)) {
            userId = resolveUserId(stripeSub);
        }

        PlanInfo planInfo = resolvePlan(resolvePlanCode(stripeSub));

        upsertSubscription(stripeSub, userId, session.getCustomer(), planInfo);
        recordStripePayment(userId, invoiceLikeOrderId(session.getId(), stripeSub.getId()), session.getCustomer(), amountFromSubscription(stripeSub), planInfo.planCode(), PaymentHistory.PaymentStatus.READY);
        forceLogout(userId);
    }

    @Transactional
    public void handleInvoicePaid(Invoice invoice) throws Exception {
        String subscriptionId = invoice.getSubscription();
        if (!StringUtils.hasText(subscriptionId)) {
            log.warn("Invoice paid event without subscription id, invoice={}", invoice.getId());
            return;
        }

        com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(subscriptionId);

        String userId = resolveUserId(stripeSub);
        PlanInfo planInfo = resolvePlan(resolvePlanCode(stripeSub));

        upsertSubscription(stripeSub, userId, stripeSub.getCustomer(), planInfo);
        recordStripePayment(userId, invoice.getId(), stripeSub.getCustomer(), invoice.getAmountPaid(), planInfo.planCode(), PaymentHistory.PaymentStatus.APPROVED);
        forceLogout(userId);
    }

    @Transactional
    public void handlePaymentFailed(Invoice invoice) throws Exception {
        String subscriptionId = invoice.getSubscription();
        if (!StringUtils.hasText(subscriptionId)) {
            log.warn("Invoice payment_failed without subscription id, invoice={}", invoice.getId());
            return;
        }

        com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(subscriptionId);

        String userId = resolveUserId(stripeSub);
        PlanInfo planInfo = resolvePlan(resolvePlanCode(stripeSub));

        upsertSubscription(stripeSub, userId, stripeSub.getCustomer(), planInfo);
        recordStripePayment(userId, invoice.getId(), stripeSub.getCustomer(), invoice.getAmountDue(), planInfo.planCode(), PaymentHistory.PaymentStatus.FAILED);
    }

    @Transactional
    public void handleSubscriptionDeleted(com.stripe.model.Subscription stripeSub) {
        String userId = resolveUserId(stripeSub);
        PlanInfo planInfo = resolvePlan(resolvePlanCode(stripeSub));

        upsertSubscription(stripeSub, userId, stripeSub.getCustomer(), planInfo);
    }

    private Subscription upsertSubscription(com.stripe.model.Subscription stripeSub,
                                            String userId,
                                            String customerId,
                                            PlanInfo planInfo) {
        if (!StringUtils.hasText(userId)) {
            log.warn("Stripe subscription {} missing user id metadata; skipping DB sync", stripeSub.getId());
            return null;
        }

        Subscription entity = subscriptionRepository.findByProviderSubscriptionId(stripeSub.getId())
                .orElseGet(() -> Subscription.builder()
                        .provider(Subscription.PaymentProvider.STRIPE)
                        .providerSubscriptionId(stripeSub.getId())
                        .userId(userId)
                        .build());

        entity.setUserId(userId);
        entity.setProvider(Subscription.PaymentProvider.STRIPE);
        entity.setPlanCode(planInfo.planCode());
        entity.setProviderCustomerId(customerId);
        entity.setStatus(stripeSub.getStatus());
        entity.setStartedAt(LocalDateTime.now());
        entity.setExpiredAt(LocalDateTime.now().plusDays(planInfo.durationDays()));
        entity.setCanceledAt(toKst(stripeSub.getCanceledAt()));

        subscriptionRepository.save(entity);
        updateMembership(userId, stripeSub.getStatus());

        return entity;
    }

    private void updateMembership(String userId, String status) {
        membershipService.syncMembership(userId);
    }

    private String resolveUserId(com.stripe.model.Subscription stripeSub) {
        Map<String, String> metadata = stripeSub.getMetadata();
        if (metadata != null && metadata.containsKey("user_id")) {
            return metadata.get("user_id");
        }
        return subscriptionRepository.findByProviderSubscriptionId(stripeSub.getId())
                .map(Subscription::getUserId)
                .orElse(null);
    }

    private void recordStripePayment(String userId,
                                     String orderId,
                                     String customerId,
                                     Long amount,
                                     String planCode,
                                     PaymentHistory.PaymentStatus status) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(orderId)) {
            return;
        }

        paymentHistoryRepository.findByOrderId(orderId)
                .or(() -> Optional.of(PaymentHistory.builder().orderId(orderId).build()))
                .ifPresent(history -> {
                    history.setUserLoginId(userId);
                    userRepository.findByUserId(userId).ifPresent(history::setUser);
                    history.setStatus(status);
                    history.setCid(customerId);
                    history.setPlanCode(planCode);
                    if (amount != null) {
                        history.setTotalAmount(amount.intValue());
                    }
                    paymentHistoryRepository.save(history);
                });
    }

    private String invoiceLikeOrderId(String sessionId, String subscriptionId) {
        if (StringUtils.hasText(sessionId)) return "stripe_session_" + sessionId;
        if (StringUtils.hasText(subscriptionId)) return "stripe_sub_" + subscriptionId;
        return "stripe_unknown";
    }

    private Long amountFromSubscription(com.stripe.model.Subscription stripeSub) {
        try {
            if (stripeSub.getItems() != null && stripeSub.getItems().getData() != null) {
                return stripeSub.getItems().getData().stream()
                        .findFirst()
                        .map(item -> item.getPlan().getAmount())
                        .orElse(null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private PlanInfo resolvePlan(String planCode) {
        if (PLAN_ANNUAL.equalsIgnoreCase(planCode)) {
            validatePriceId(stripePriceIdAnnual, "STRIPE_PRICE_ID_ANNUAL");
            return new PlanInfo(PLAN_ANNUAL, stripePriceIdAnnual, 365);
        }
        validatePriceId(stripePriceIdMonthly, "STRIPE_PRICE_ID_MONTHLY");
        return new PlanInfo(PLAN_MONTHLY, stripePriceIdMonthly, 30);
    }

    private String resolvePlanCode(com.stripe.model.Subscription stripeSub) {
        Map<String, String> metadata = stripeSub.getMetadata();
        if (metadata != null && metadata.containsKey("plan_code")) {
            return metadata.get("plan_code");
        }
        if (stripeSub.getItems() != null && stripeSub.getItems().getData() != null) {
            String priceId = stripeSub.getItems().getData().stream()
                    .findFirst()
                    .map(item -> item.getPrice().getId())
                    .orElse(null);
            if (stripePriceIdAnnual.equals(priceId)) return PLAN_ANNUAL;
            if (stripePriceIdMonthly.equals(priceId)) return PLAN_MONTHLY;
        }
        return PLAN_MONTHLY;
    }

    private record PlanInfo(String planCode, String priceId, int durationDays) {
    }

    private void validatePriceId(String priceId, String configKey) {
        if (!StringUtils.hasText(priceId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Stripe price id 설정을 확인하세요: " + configKey);
        }
    }

    private void forceLogout(String userId) {
        jwtUtil.deleteRefreshToken(userId);
        jwtUtil.forceLogoutUser(userId);
    }

    private LocalDateTime toKST(Long timestamp) {
        if (timestamp == null) return null;
        return Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null
                || authentication.getPrincipal().equals("anonymousUser")) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "로그인이 필요합니다");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserPrincipal customUserPrincipal) {
            return userRepository.findByUserId(customUserPrincipal.getUserId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: " + customUserPrincipal.getUserId()));
        }
        throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "인증 정보를 확인할 수 없습니다");
    }

    private LocalDateTime toKst(Long epochSeconds) {
    if (epochSeconds == null) return null;
    return Instant.ofEpochSecond(epochSeconds)
            .atZone(ZoneId.of("Asia/Seoul"))
            .toLocalDateTime();
    }
}
