package rto.intelfit.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rto.intelfit.service.StripeService;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/stripe")
public class StripeWebhookController {

    private final StripeService stripeService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) throws Exception {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        }

        String eventType = event.getType();
        log.info("Stripe event received: {}", eventType);

        switch (eventType) {
            case "checkout.session.completed" -> {
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                Optional<com.stripe.model.StripeObject> object = deserializer.getObject();
                if (object.isEmpty()) break;
                stripeService.handleCheckoutCompleted((Session) object.get());
            }
            case "invoice.paid" -> {
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                Optional<com.stripe.model.StripeObject> object = deserializer.getObject();
                if (object.isEmpty()) break;
                stripeService.handleInvoicePaid((Invoice) object.get());
            }
            case "invoice.payment_failed" -> {
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                Optional<com.stripe.model.StripeObject> object = deserializer.getObject();
                if (object.isEmpty()) break;
                stripeService.handlePaymentFailed((Invoice) object.get());
            }
            case "customer.subscription.deleted" -> {
                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                Optional<com.stripe.model.StripeObject> object = deserializer.getObject();
                if (object.isEmpty()) break;
                stripeService.handleSubscriptionDeleted((com.stripe.model.Subscription) object.get());
            }
            default -> log.debug("Unhandled Stripe event type: {}", eventType);
        }

        return ResponseEntity.ok("ok");
    }
}
