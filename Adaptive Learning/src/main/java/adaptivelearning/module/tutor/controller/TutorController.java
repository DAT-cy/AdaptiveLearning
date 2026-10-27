package adaptivelearning.module.tutor.controller;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.billing.service.EntitlementService;
import adaptivelearning.module.tutor.dto.request.AskTutorRequest;
import adaptivelearning.module.tutor.dto.request.CreateConversationRequest;
import adaptivelearning.module.tutor.service.TutorService;
import adaptivelearning.utils.ClientUtils;
import adaptivelearning.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ClientUtils.VERSION + "/tutor")
@Tag(name = "AI Tutor")
@RequiredArgsConstructor
public class TutorController {
    private final TutorService service;
    private final EntitlementService entitlementService;

    private Long user() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            entitlementService.requireAi(userId);
            return userId;
        } catch (CommonException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.FORBIDDEN_ERROR);
        }
    }

    @PostMapping("/conversations")
    public ResponseEntity<DefaultRes<?>> create(@RequestBody CreateConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DefaultRes.res(201, "CREATED", service.createConversation(user(), request)));
    }

    @GetMapping("/conversations")
    public DefaultRes<?> list() {
        return DefaultRes.res(200, "GET LIST", service.getConversations(user()));
    }

    @GetMapping("/conversations/{id}/messages")
    public DefaultRes<?> messages(@PathVariable Long id) {
        return DefaultRes.res(200, "GET LIST", service.getMessages(user(), id));
    }

    @PostMapping("/conversations/{id}/messages")
    public DefaultRes<?> ask(@PathVariable Long id, @Valid @RequestBody AskTutorRequest request) {
        return DefaultRes.res(200, "SUCCESS", service.ask(user(), id, request));
    }

    @DeleteMapping("/conversations/{id}")
    public DefaultRes<Void> archive(@PathVariable Long id) {
        service.archive(user(), id);
        return DefaultRes.res(200, "DELETED");
    }
}
