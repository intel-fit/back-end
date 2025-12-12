package rto.intelfit.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rto.intelfit.service.StripeService;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/stripe")
public class StripeWebhookController {

    private final StripeService stripeService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) {
        String payload;
        try {
            payload = request.getReader()
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.error("Failed to read Stripe webhook payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        }

        String sigHeader = request.getHeader("Stripe-Signature");
        if (sigHeader == null) {
            log.error("Missing Stripe-Signature header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        }

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        }

        String eventType = event.getType();
        log.info("Stripe webhook event received: {}", eventType);

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<com.stripe.model.StripeObject> object = deserializer.getObject();

        com.stripe.model.StripeObject stripeObject = object.orElseGet(() -> {
            log.warn("Stripe deserializer returned empty, using raw object");
            return event.getData().getObject();
        });

        switch (eventType) {
            case "checkout.session.completed" -> {
                Session session = (Session) stripeObject;
                stripeService.handleCheckoutCompleted(session);
            }
            case "invoice.paid" -> {
                Invoice invoice = (Invoice) stripeObject;
                stripeService.handleInvoicePaid(invoice);
            }
            case "invoice.payment_failed" -> {
                Invoice invoice = (Invoice) stripeObject;
                stripeService.handlePaymentFailed(invoice);
            }
            case "customer.subscription.deleted" -> {
                com.stripe.model.Subscription sub =
                        (com.stripe.model.Subscription) stripeObject;
                stripeService.handleSubscriptionDeleted(sub);
            }
            default -> log.debug("Unhandled Stripe event type: {}", eventType);
        }

        return ResponseEntity.ok("ok");
    }
}
