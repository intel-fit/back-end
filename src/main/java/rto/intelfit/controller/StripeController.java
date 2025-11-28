package rto.intelfit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rto.intelfit.dto.StripeCheckoutResponse;
import rto.intelfit.dto.SubscriptionStatusDto;
import rto.intelfit.service.StripeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/stripe")
public class StripeController {

    private final StripeService stripeService;

    @GetMapping("/subscription-status")
    public ResponseEntity<SubscriptionStatusDto> getStatus() {
        return ResponseEntity.ok(stripeService.getSubscriptionStatusForCurrentUser());
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<StripeCheckoutResponse> createCheckoutSession(@RequestParam(defaultValue = "PREMIUM_MONTHLY") String planCode) throws Exception {
        return ResponseEntity.ok(stripeService.createCheckoutSession(planCode));
    }

    @GetMapping("/success")
    public ResponseEntity<String> handleSuccess(@RequestParam(name = "session_id", required = false) String sessionId) throws Exception {
        if (sessionId != null) {
            stripeService.processCheckoutSuccess(sessionId);
        }
        return ResponseEntity.ok("Stripe checkout succeeded" + (sessionId != null ? " (session=" + sessionId + ")" : ""));
    }

    @GetMapping("/cancel")
    public ResponseEntity<String> handleCancel() {
        return ResponseEntity.ok("Stripe checkout canceled");
    }
}
