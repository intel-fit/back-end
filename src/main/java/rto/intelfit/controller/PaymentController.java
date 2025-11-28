package rto.intelfit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rto.intelfit.dto.KakaoPayPaymentDto;
import rto.intelfit.service.KakaoPayPaymentService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/kakaopay")
@Tag(name = "KakaoPay API", description = "카카오페이 Ready → Redirect → Approve 결제 플로우")
public class PaymentController {

    private final KakaoPayPaymentService kakaoPayPaymentService;

    @Operation(summary = "카카오페이 결제 준비", description = "카카오페이 결제창으로 이동할 리다이렉트 URL을 발급합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 준비 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 오류 또는 결제 준비 실패"),
            @ApiResponse(responseCode = "403", description = "카카오페이 Admin Key 미설정/오류")
    })
    @PostMapping("/ready")
    public ResponseEntity<KakaoPayPaymentDto.ReadyResponse> ready(
            @Valid @RequestBody KakaoPayPaymentDto.ReadyRequest request
    ) {
        log.info("카카오페이 결제 준비 요청 - planCode={}", request.getPlanCode());

        KakaoPayPaymentDto.ReadyResponse response = kakaoPayPaymentService.readyPayment(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "카카오페이 결제 승인", description = "카카오페이에서 전달한 pg_token으로 결제를 승인합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 승인 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 결제 세션 또는 승인 실패"),
            @ApiResponse(responseCode = "403", description = "카카오페이 Admin Key 미설정/오류")
    })
    @GetMapping("/approve")
    public ResponseEntity<KakaoPayPaymentDto.ApproveResponse> approve(
            @RequestParam("orderId") String orderId,
            @RequestParam("userId") String userId,
            @RequestParam("pg_token") String pgToken
    ) {
        log.info("카카오페이 결제 승인 요청 - orderId={}, userId={}", orderId, userId);

        KakaoPayPaymentDto.ApproveRequest approveRequest = KakaoPayPaymentDto.ApproveRequest.builder()
                .orderId(orderId)
                .userId(userId)
                .pgToken(pgToken)
                .build();

        KakaoPayPaymentDto.ApproveResponse response = kakaoPayPaymentService.approvePayment(approveRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "카카오페이 결제 취소 리다이렉트 처리", description = "사용자가 카카오페이 창에서 결제를 취소했을 때의 응답을 반환합니다")
    @GetMapping("/cancel")
    public ResponseEntity<KakaoPayPaymentDto.RedirectResult> cancel(
            @RequestParam(value = "orderId", required = false) String orderId
    ) {
        KakaoPayPaymentDto.RedirectResult response = kakaoPayPaymentService.handleCancel(orderId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "카카오페이 결제 실패 리다이렉트 처리", description = "카카오페이 결제가 실패했을 때의 응답을 반환합니다")
    @GetMapping("/fail")
    public ResponseEntity<KakaoPayPaymentDto.RedirectResult> fail(
            @RequestParam(value = "orderId", required = false) String orderId
    ) {
        KakaoPayPaymentDto.RedirectResult response = kakaoPayPaymentService.handleFail(orderId);
        return ResponseEntity.ok(response);
    }
}
