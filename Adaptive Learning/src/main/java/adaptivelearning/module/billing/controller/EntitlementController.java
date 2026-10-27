package adaptivelearning.module.billing.controller;

import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.module.billing.dto.EntitlementsResponse;
import adaptivelearning.module.billing.service.EntitlementService;
import adaptivelearning.utils.ClientUtils;
import adaptivelearning.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ClientUtils.VERSION + "/me/entitlements")
@RequiredArgsConstructor
public class EntitlementController {
    private final EntitlementService entitlementService;

    @GetMapping
    public ResponseEntity<DefaultRes<EntitlementsResponse>> current() {
        return ResponseEntity.ok(DefaultRes.res(200, "Current entitlements",
                entitlementService.getEntitlements(SecurityUtils.getCurrentUserId())));
    }
}
