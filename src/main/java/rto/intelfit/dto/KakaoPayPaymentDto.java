package rto.intelfit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "KakaoPayPaymentDto", description = "카카오페이 결제 요청/응답 DTO")
public class KakaoPayPaymentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카카오페이 결제 준비 요청 (주문ID/사용자ID/금액은 서버가 결정)")
    public static class ReadyRequest {
        @Schema(description = "상품/플랜 코드 (서버에서 금액·상품명을 결정)", example = "PREMIUM_MONTHLY")
        private String planCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카카오페이 결제 준비 응답")
    public static class ReadyResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "카카오페이 결제 고유 번호(TID)", example = "T1234567890")
        private String tid;

        @Schema(description = "카카오페이 결제 리다이렉트 URL (PC/Mobile)", example = "https://online-pay.kakao.com/...")
        private String redirectUrl;

        @Schema(description = "상점 주문 번호", example = "ORDER-20241010-001")
        private String orderId;

        @Schema(description = "상점 사용자 ID", example = "user-1234")
        private String userId;

        @Schema(description = "응답 메시지", example = "카카오페이 결제창으로 이동하세요")
        private String message;

        @Schema(description = "카카오 응답 생성 시각", example = "2024-10-10T12:00:00")
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카카오페이 결제 승인 요청")
    public static class ApproveRequest {
        @NotBlank
        @Schema(description = "상점 주문 번호 (partner_order_id)", example = "ORDER-20241010-001")
        private String orderId;

        @NotBlank
        @JsonProperty("pg_token")
        @Schema(description = "카카오페이가 전달한 PG 토큰", example = "T1234567890")
        private String pgToken;

        @NotBlank
        @Schema(description = "상점 사용자 ID (partner_user_id)", example = "user-1234")
        private String userId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카카오페이 결제 승인 응답")
    public static class ApproveResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "응답 메시지", example = "결제가 완료되었습니다")
        private String message;

        @Schema(description = "요청 고유 번호", example = "A1234567890")
        private String aid;

        @Schema(description = "카카오페이 결제 고유 번호", example = "T1234567890")
        private String tid;

        @Schema(description = "가맹점 코드", example = "TC0ONETIME")
        private String cid;

        @Schema(description = "상점 주문 번호", example = "ORDER-20241010-001")
        private String orderId;

        @Schema(description = "상점 사용자 ID", example = "user-1234")
        private String userId;

        @Schema(description = "승인 시각", example = "2024-10-10T12:05:00")
        private String approvedAt;

        @Schema(description = "총 결제 금액", example = "1000")
        private Integer totalAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedirectResult {
        private boolean success;
        private String status;
        private String orderId;
        private String message;
    }

    // ===== 카카오 API 원본 응답 DTO =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoPayReadyResponse {
        private String tid;

        @JsonProperty("next_redirect_app_url")
        private String nextRedirectAppUrl;

        @JsonProperty("next_redirect_mobile_url")
        private String nextRedirectMobileUrl;

        @JsonProperty("next_redirect_pc_url")
        private String nextRedirectPcUrl;

        @JsonProperty("created_at")
        private String createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoPayApproveResponse {
        private String aid;
        private String tid;
        private String cid;

        @JsonProperty("partner_order_id")
        private String partnerOrderId;

        @JsonProperty("partner_user_id")
        private String partnerUserId;

        @JsonProperty("approved_at")
        private String approvedAt;

        private Amount amount;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Amount {
            @JsonProperty("total")
            private Integer total;
        }
    }
}
