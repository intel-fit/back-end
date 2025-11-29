package rto.intelfit.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import rto.intelfit.dto.KakaoPayPaymentDto;
import rto.intelfit.exception.BusinessException;
import rto.intelfit.exception.ErrorCode;
import rto.intelfit.domain.PaymentHistory;
import rto.intelfit.domain.Subscription;
import rto.intelfit.domain.User;
import rto.intelfit.repository.PaymentHistoryRepository;
import rto.intelfit.repository.SubscriptionRepository;
import rto.intelfit.repository.UserRepository;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.MembershipService;
import rto.intelfit.util.JwtUtil;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KakaoPayPaymentService {

    private static final String PLAN_MONTHLY = "PREMIUM_MONTHLY";
    private static final String PLAN_ANNUAL = "PREMIUM_ANNUAL";

    private final WebClient webClient;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;
    private final SubscriptionRepository subscriptionRepository;
    private final JwtUtil jwtUtil;

    @Value("${kakaopay.cid}")
    private String cid;

    @Value("${kakaopay.admin-key}")
    private String adminKey;

    @Value("${kakaopay.ready-url}")
    private String readyUrl;

    @Value("${kakaopay.approve-url}")
    private String approveUrl;

    @Value("${kakaopay.redirect.approval}")
    private String approvalRedirectUrl;

    @Value("${kakaopay.redirect.cancel}")
    private String cancelRedirectUrl;

    @Value("${kakaopay.redirect.fail}")
    private String failRedirectUrl;

    @Value("${kakaopay.plan.monthly.item-name}")
    private String monthlyItemName;

    @Value("${kakaopay.plan.monthly.amount:5900}")
    private Integer monthlyAmount;

    @Value("${kakaopay.plan.monthly.tax-free-amount:0}")
    private Integer monthlyTaxFreeAmount;

    @Value("${kakaopay.plan.annual.item-name}")
    private String annualItemName;

    @Value("${kakaopay.plan.annual.amount:59000}")
    private Integer annualAmount;

    @Value("${kakaopay.plan.annual.tax-free-amount:0}")
    private Integer annualTaxFreeAmount;

    // 간단한 세션 저장소 (orderId -> tid, userId)
    private final Map<String, PaymentSession> paymentSessionStore = new ConcurrentHashMap<>();

    @Transactional
    public KakaoPayPaymentDto.ReadyResponse readyPayment(KakaoPayPaymentDto.ReadyRequest request) {
        validateConfig();

        User user = getAuthenticatedUser();

        PlanInfo plan = resolvePlan(request.getPlanCode());

        String generatedOrderId = generateOrderId();
        int quantity = 1;
        int taxFreeAmount = Optional.ofNullable(plan.taxFreeAmount()).orElse(0);

        String approvalUrl = buildRedirectUrl(approvalRedirectUrl, generatedOrderId, user.getUserId());
        String cancelUrl = buildRedirectUrl(cancelRedirectUrl, generatedOrderId, user.getUserId());
        String failUrl = buildRedirectUrl(failRedirectUrl, generatedOrderId, user.getUserId());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("cid", cid);
        formData.add("partner_order_id", generatedOrderId);
        formData.add("partner_user_id", user.getUserId());
        formData.add("item_name", plan.itemName());
        formData.add("quantity", String.valueOf(quantity));
        formData.add("total_amount", String.valueOf(plan.amount()));
        formData.add("tax_free_amount", String.valueOf(taxFreeAmount));
        formData.add("approval_url", approvalUrl);
        formData.add("cancel_url", cancelUrl);
        formData.add("fail_url", failUrl);

        KakaoPayPaymentDto.KakaoPayReadyResponse kakaoResponse = requestToKakaoPay(
                readyUrl,
                formData,
                KakaoPayPaymentDto.KakaoPayReadyResponse.class,
                ErrorCode.KAKAOPAY_READY_FAILED
        );

        PaymentHistory history = paymentHistoryRepository.findByOrderId(generatedOrderId)
                .orElse(PaymentHistory.builder().orderId(generatedOrderId).build());

        history.setTid(kakaoResponse.getTid());
        history.setUser(user);
        history.setUserLoginId(user.getUserId());
        history.setItemName(plan.itemName());
        history.setPlanCode(plan.planCode());
        history.setQuantity(quantity);
        history.setTotalAmount(plan.amount());
        history.setTaxFreeAmount(taxFreeAmount);
        history.setStatus(PaymentHistory.PaymentStatus.READY);
        paymentHistoryRepository.save(history);

        paymentSessionStore.put(generatedOrderId,
                new PaymentSession(kakaoResponse.getTid(), user.getUserId(), plan.planCode()));

        String redirectUrl = selectRedirectUrl(kakaoResponse);

        return KakaoPayPaymentDto.ReadyResponse.builder()
                .success(true)
                .tid(kakaoResponse.getTid())
                .redirectUrl(redirectUrl)
                .orderId(generatedOrderId)
                .userId(user.getUserId())
                .message("카카오페이 결제창으로 이동하세요")
                .createdAt(kakaoResponse.getCreatedAt())
                .build();
    }

    @Transactional
    public KakaoPayPaymentDto.ApproveResponse approvePayment(KakaoPayPaymentDto.ApproveRequest request) {
        validateConfig();

        PaymentSession session = Optional.ofNullable(paymentSessionStore.get(request.getOrderId()))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.KAKAOPAY_SESSION_NOT_FOUND,
                        "결제 정보가 만료되었거나 존재하지 않습니다. orderId=" + request.getOrderId()
                ));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("cid", cid);
        formData.add("tid", session.tid());
        formData.add("partner_order_id", request.getOrderId());
        formData.add("partner_user_id", session.userId());
        formData.add("pg_token", request.getPgToken());

        KakaoPayPaymentDto.KakaoPayApproveResponse kakaoResponse = requestToKakaoPay(
                approveUrl,
                formData,
                KakaoPayPaymentDto.KakaoPayApproveResponse.class,
                ErrorCode.KAKAOPAY_APPROVE_FAILED
        );

        PaymentHistory history = paymentHistoryRepository.findByOrderId(request.getOrderId())
                .orElseGet(() -> PaymentHistory.builder()
                        .orderId(request.getOrderId())
                        .tid(session.tid())
                        .userLoginId(session.userId())
                        .planCode(null)
                        .status(PaymentHistory.PaymentStatus.READY)
                        .build());

        history.setAid(kakaoResponse.getAid());
        history.setTid(kakaoResponse.getTid());
        history.setCid(kakaoResponse.getCid());
        history.setApprovedAt(kakaoResponse.getApprovedAt());
        history.setStatus(PaymentHistory.PaymentStatus.APPROVED);
        history.setTotalAmount(kakaoResponse.getAmount() != null ? kakaoResponse.getAmount().getTotal() : history.getTotalAmount());
        if (!StringUtils.hasText(history.getPlanCode())) {
            history.setPlanCode(session.planCode());
        }

        User user = findUser(session.userId());
        history.setUser(user);
        history.setUserLoginId(user.getUserId());
        paymentHistoryRepository.save(history);

        PlanInfo plan = resolvePlan(session.planCode());
        upsertKakaoSubscription(user.getUserId(), session.tid(), plan);
        membershipService.syncMembership(user.getUserId());
        forceLogout(user.getUserId());

        paymentSessionStore.remove(request.getOrderId());

        Integer totalAmount = kakaoResponse.getAmount() != null
                ? kakaoResponse.getAmount().getTotal()
                : null;

        return KakaoPayPaymentDto.ApproveResponse.builder()
                .success(true)
                .message("결제가 완료되었습니다")
                .aid(kakaoResponse.getAid())
                .tid(kakaoResponse.getTid())
                .cid(kakaoResponse.getCid())
                .orderId(kakaoResponse.getPartnerOrderId())
                .userId(kakaoResponse.getPartnerUserId())
                .approvedAt(kakaoResponse.getApprovedAt())
                .totalAmount(totalAmount)
                .build();
    }

    @Transactional
    public KakaoPayPaymentDto.RedirectResult handleCancel(String orderId) {
        paymentSessionStore.remove(orderId);
        if (StringUtils.hasText(orderId)) {
            paymentHistoryRepository.findByOrderId(orderId)
                    .ifPresent(history -> {
                        history.setStatus(PaymentHistory.PaymentStatus.CANCELLED);
                        paymentHistoryRepository.save(history);
                    });
        }
        return new KakaoPayPaymentDto.RedirectResult(false, "CANCEL", orderId, "사용자가 결제를 취소했습니다");
    }

    @Transactional
    public KakaoPayPaymentDto.RedirectResult handleFail(String orderId) {
        paymentSessionStore.remove(orderId);
        if (StringUtils.hasText(orderId)) {
            paymentHistoryRepository.findByOrderId(orderId)
                    .ifPresent(history -> {
                        history.setStatus(PaymentHistory.PaymentStatus.FAILED);
                        paymentHistoryRepository.save(history);
                    });
        }
        return new KakaoPayPaymentDto.RedirectResult(false, "FAIL", orderId, "결제에 실패했습니다");
    }

    private void validateConfig() {
        if (!StringUtils.hasText(adminKey)) {
            throw new BusinessException(ErrorCode.KAKAOPAY_CONFIG_MISSING, "카카오페이 Admin Key가 설정되지 않았습니다");
        }
        if (monthlyAmount == null || monthlyAmount <= 0 || annualAmount == null || annualAmount <= 0) {
            throw new BusinessException(ErrorCode.KAKAOPAY_CONFIG_MISSING, "월/연 결제 금액 설정을 확인해주세요");
        }
    }

    private String buildRedirectUrl(String baseUrl, String orderId, String userId) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("orderId", orderId)
                .queryParam("userId", userId)
                .build()
                .toUriString();
    }

    private String selectRedirectUrl(KakaoPayPaymentDto.KakaoPayReadyResponse response) {
        if (StringUtils.hasText(response.getNextRedirectPcUrl())) {
            return response.getNextRedirectPcUrl();
        }
        if (StringUtils.hasText(response.getNextRedirectMobileUrl())) {
            return response.getNextRedirectMobileUrl();
        }
        return response.getNextRedirectAppUrl();
    }

    private <T> T requestToKakaoPay(String url,
                                    MultiValueMap<String, String> formData,
                                    Class<T> responseType,
                                    ErrorCode errorCode) {
        try {
            return webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (WebClientResponseException.Forbidden e) {
            log.warn("카카오페이 403 응답 - Admin Key 확인 필요, uri={}, status={}", url, e.getStatusCode());
            throw new BusinessException(ErrorCode.KAKAOPAY_FORBIDDEN, "카카오페이 요청이 거부되었습니다. Admin Key를 확인해주세요");
        } catch (WebClientResponseException e) {
            log.error("카카오페이 API 호출 실패 uri={}, status={}, body={}", url, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(errorCode,
                    "카카오페이 API 호출 실패: " + e.getStatusCode().value() + " " + e.getStatusText(),
                    e);
        } catch (Exception e) {
            log.error("카카오페이 API 호출 중 알 수 없는 오류 uri={}", url, e);
            throw new BusinessException(errorCode, "카카오페이 API 호출 중 오류가 발생했습니다", e);
        }
    }

    private record PaymentSession(String tid, String userId, String planCode) {
    }

    private User findUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null
                || authentication.getPrincipal().equals("anonymousUser")) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "로그인이 필요합니다");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserPrincipal customUserPrincipal) {
            return findUser(customUserPrincipal.getUserId());
        }
        throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "인증 정보를 확인할 수 없습니다");
    }

    private String generateOrderId() {
        return "ORDER-" + UUID.randomUUID();
    }

    private void upsertKakaoSubscription(String userId, String tid, PlanInfo plan) {
        Subscription subscription = subscriptionRepository
                .findTopByUserIdAndProviderOrderByCreatedAtDesc(userId, Subscription.PaymentProvider.KAKAOPAY)
                .orElseGet(() -> Subscription.builder()
                        .userId(userId)
                        .provider(Subscription.PaymentProvider.KAKAOPAY)
                        .providerSubscriptionId(tid)
                        .build());

        subscription.setProvider(Subscription.PaymentProvider.KAKAOPAY);
        subscription.setProviderSubscriptionId(tid);
        subscription.setStatus("active");
        subscription.setPlanCode(plan.planCode());
        subscription.setStartedAt(LocalDateTime.now());
        subscription.setExpiredAt(LocalDateTime.now().plusDays(plan.durationDays()));

        subscriptionRepository.save(subscription);
    }

    private PlanInfo resolvePlan(String planCode) {
        if ("PREMIUM_ANNUAL".equalsIgnoreCase(planCode)) {
            return new PlanInfo("PREMIUM_ANNUAL", annualItemName, annualAmount, annualTaxFreeAmount, 365);
        }
        return new PlanInfo("PREMIUM_MONTHLY", monthlyItemName, monthlyAmount, monthlyTaxFreeAmount, 30);
    }

    private record PlanInfo(String planCode, String itemName, Integer amount, Integer taxFreeAmount, int durationDays) {
    }

    private void forceLogout(String userId) {
        jwtUtil.deleteRefreshToken(userId);
        jwtUtil.forceLogoutUser(userId);
    }
}
