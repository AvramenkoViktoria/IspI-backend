package org.docpirates.ispi.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.docpirates.ispi.service.ContactInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text")
public class TextValidationController {

    @PostMapping("/validate-contact")
    public ResponseEntity<ValidationResponse> validateContact(@RequestBody ValidationRequest request) {
        if (request.getText() == null || request.getText().isBlank())
            return ResponseEntity.badRequest().body(new ValidationResponse(false, "Text must not be empty"));

        boolean hasContact = ContactInfoService.containsContactInfo(request.getText());
        if (hasContact)
            return ResponseEntity.badRequest().body(new ValidationResponse(false, "Text contains contact info"));

        return ResponseEntity.ok(new ValidationResponse(true, "Text is OK"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationRequest {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResponse {
        private boolean valid;
        private String message;
    }
}
