package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rto.intelfit.dto.SubscriptionDto;
import rto.intelfit.security.CustomUserPrincipal;
import rto.intelfit.service.SubscriptionService;

@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription API", description = "구독 정보 조회 및 취소 API")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    @Operation(summary = "내 구독 정보 조회", description = "현재 로그인한 사용자의 구독 정보를 조회합니다")
    public ResponseEntity<SubscriptionDto.SubscriptionInfoResponse> getSubscription(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("구독 정보 조회 요청 - userId={}", userPrincipal.getUserId());
        return ResponseEntity.ok(subscriptionService.getSubscriptionInfo(userPrincipal));
    }

    @PostMapping("/cancel")
    @Operation(summary = "구독 취소", description = "현재 활성화된 구독을 취소합니다")
    public ResponseEntity<SubscriptionDto.SubscriptionCancelResponse> cancelSubscription(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        log.info("구독 취소 요청 - userId={}", userPrincipal.getUserId());
        return ResponseEntity.ok(subscriptionService.cancelSubscription(userPrincipal));
    }
}
