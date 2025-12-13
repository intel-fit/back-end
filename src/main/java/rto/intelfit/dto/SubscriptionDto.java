package rto.intelfit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rto.intelfit.domain.Subscription;

import java.time.LocalDateTime;

@Schema(name = "SubscriptionDto", description = "구독 정보/취소 응답 DTO")
public class SubscriptionDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "구독 상세 정보 응답")
    public static class SubscriptionInfoResponse {

        @Schema(description = "활성 구독 여부", example = "true")
        private boolean hasActiveSubscription;

        @Schema(description = "구독 상태", example = "active")
        private String status;

        @Schema(description = "구독 플랜 코드", example = "PREMIUM_MONTHLY")
        private String planCode;

        @Schema(description = "구독 시작일시")
        private LocalDateTime startedAt;

        @Schema(description = "구독 만료일시")
        private LocalDateTime expiredAt;

        @Schema(description = "구독 취소일시")
        private LocalDateTime canceledAt;

        @Schema(description = "결제 제공자", example = "KAKAOPAY")
        private Subscription.PaymentProvider provider;

        @Schema(description = "결제사 고객 ID", example = "cus_xxxxx")
        private String providerCustomerId;

        @Schema(description = "결제사 구독 ID", example = "sub_xxxxx")
        private String providerSubscriptionId;

        public static SubscriptionInfoResponse from(Subscription subscription) {
            LocalDateTime now = LocalDateTime.now();
            boolean active = subscription.getExpiredAt() != null
                    && subscription.getExpiredAt().isAfter(now)
                    && !"canceled".equalsIgnoreCase(subscription.getStatus());

            return SubscriptionInfoResponse.builder()
                    .hasActiveSubscription(active)
                    .status(subscription.getStatus())
                    .planCode(subscription.getPlanCode())
                    .startedAt(subscription.getStartedAt())
                    .expiredAt(subscription.getExpiredAt())
                    .canceledAt(subscription.getCanceledAt())
                    .provider(subscription.getProvider())
                    .providerCustomerId(subscription.getProviderCustomerId())
                    .providerSubscriptionId(subscription.getProviderSubscriptionId())
                    .build();
        }

        public static SubscriptionInfoResponse empty() {
            return SubscriptionInfoResponse.builder()
                    .hasActiveSubscription(false)
                    .status("none")
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "구독 취소 응답")
    public static class SubscriptionCancelResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "응답 메시지", example = "구독이 취소되었습니다")
        private String message;

        @Schema(description = "취소 후 구독 정보")
        private SubscriptionInfoResponse subscription;
    }
}
