package adaptivelearning.module.billing.controller;

import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.module.billing.dto.CheckoutResponse;
import adaptivelearning.module.billing.dto.PaymentCheckoutRequest;
import adaptivelearning.module.billing.dto.PaymentStatusResponse;
import adaptivelearning.module.billing.service.PaymentService;
import adaptivelearning.utils.ClientUtils;
import adaptivelearning.utils.SecurityUtils;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ClientUtils.VERSION + "/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/checkout")
    public ResponseEntity<DefaultRes<CheckoutResponse>> checkout(
            @Valid @RequestBody PaymentCheckoutRequest request) {
        CheckoutResponse result = paymentService.checkout(SecurityUtils.getCurrentUserId(), request.getProvider());
        return ResponseEntity.status(HttpStatus.CREATED).body(DefaultRes.res(201, "Checkout created", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DefaultRes<PaymentStatusResponse>> status(@PathVariable Long id) {
        return ResponseEntity.ok(DefaultRes.res(200, "Payment status", paymentService.getPayment(
                id, SecurityUtils.getCurrentUserId())));
    }

    @GetMapping("/history")
    public ResponseEntity<DefaultRes<java.util.List<PaymentStatusResponse>>> history() {
        return ResponseEntity.ok(DefaultRes.res(200, "Recent payments",
                paymentService.getRecentPayments(SecurityUtils.getCurrentUserId())));
    }

    @GetMapping("/paypal/return")
    public ResponseEntity<Void> paypalReturn(@RequestParam String token) {
        try {
            Long paymentId = paymentService.confirmPayPalReturn(token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(paymentService.paymentReturnUrl(paymentId))).build();
        } catch (RuntimeException exception) {
            try {
                Long paymentId = paymentService.getPayPalPaymentId(token);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(paymentService.paymentReturnUrl(paymentId))).build();
            } catch (RuntimeException notFound) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(paymentService.paymentCancelUrl())).build();
            }
        }
    }

    @PostMapping("/sandbox/complete")
    public ResponseEntity<DefaultRes<PaymentStatusResponse>> completeSandbox(
            @RequestBody java.util.Map<String, Object> body) {
        Long paymentId = Long.valueOf(body.get("paymentId").toString());
        boolean success = body.get("success") == null || Boolean.parseBoolean(body.get("success").toString());
        Long id = paymentService.completeSandboxPayment(paymentId, SecurityUtils.getCurrentUserId(), success);
        return ResponseEntity.ok(DefaultRes.res(200, "Sandbox payment completed",
                paymentService.getPayment(id, SecurityUtils.getCurrentUserId())));
    }
}
