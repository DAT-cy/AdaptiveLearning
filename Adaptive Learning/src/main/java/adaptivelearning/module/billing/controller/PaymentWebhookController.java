package adaptivelearning.module.billing.controller;

import adaptivelearning.module.billing.service.PaymentService;
import adaptivelearning.shared.enums.PaymentProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {
    private final PaymentService paymentService;

    @PostMapping(value = "/stripe", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> stripe(HttpServletRequest request) throws IOException {
        paymentService.handleWebhook(PaymentProvider.STRIPE, request.getInputStream().readAllBytes(), headers(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/paypal", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> paypal(HttpServletRequest request) throws IOException {
        paymentService.handleWebhook(PaymentProvider.PAYPAL, request.getInputStream().readAllBytes(), headers(request));
        return ResponseEntity.noContent().build();
    }

    private HttpHeaders headers(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames()).forEach(name ->
                headers.put(name, Collections.list(request.getHeaders(name))));
        return headers;
    }
}
