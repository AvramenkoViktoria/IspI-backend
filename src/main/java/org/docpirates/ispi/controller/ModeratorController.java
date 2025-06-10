package org.docpirates.ispi.controller;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.ComplaintDto;
import org.docpirates.ispi.dto.DocumentComplaintDto;
import org.docpirates.ispi.entity.*;
import org.docpirates.ispi.repository.ComplaintRepository;
import org.docpirates.ispi.repository.DocumentComplaintRepository;
import org.docpirates.ispi.repository.PostErrorRepository;
import org.docpirates.ispi.repository.ProfileErrorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/moderators")
@RequiredArgsConstructor
public class ModeratorController {

    private final ProfileErrorRepository profileErrorRepository;
    private final PostErrorRepository postErrorRepository;
    private final ComplaintRepository complaintRepository;
    private final UserMeController userMeController;
    private final DocumentComplaintRepository documentComplaintRepository;
    private final AuthController authController;

    // ============================== GET ============================== //

    @GetMapping("/profile-errors")
    public ResponseEntity<List<ProfileError>> getAllProfileErrors(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(authResult.getStatusCode()).body(null);
        List<ProfileError> errors = profileErrorRepository.findAll();
        return ResponseEntity.ok(errors);
    }

    @GetMapping("/post-errors")
    public ResponseEntity<List<PostError>> getAllPostErrors(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(authResult.getStatusCode()).body(null);
        List<PostError> postErrors = postErrorRepository.findAll();
        return ResponseEntity.ok(postErrors);
    }

    @GetMapping("/complaints")
    public ResponseEntity<List<ComplaintDto>> getComplaints(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String plaintiff,
            @RequestParam(required = false) String status
    ) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(authResult.getStatusCode()).body(null);

        List<Complaint> complaints = complaintRepository.findAll().stream()
                .filter(c -> {
                    boolean plaintiffMatch = true;
                    if ("student".equalsIgnoreCase(plaintiff)) {
                        plaintiffMatch = c.getPlaintiff() instanceof Student;
                    } else if ("professor".equalsIgnoreCase(plaintiff)) {
                        plaintiffMatch = c.getPlaintiff() instanceof Teacher;
                    }
                    boolean statusMatch = status == null || c.getStatus().equalsIgnoreCase(status);
                    return plaintiffMatch && statusMatch;
                })
                .toList();

        List<ComplaintDto> dtos = complaints.stream()
                .map(ComplaintDto::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/document-complaints")
    public ResponseEntity<?> getAllDocumentComplaints(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(authResult.getStatusCode()).body(null);

        List<DocumentComplaint> complaints = documentComplaintRepository.findAll();
        if (complaints.isEmpty())
            return ResponseEntity.noContent().build();

        List<DocumentComplaintDto> dtos = complaints.stream()
                .map(complaint -> new DocumentComplaintDto(
                        complaint.getId(),
                        complaint.getDocument().getId(),
                        complaint.getDocument().getName(),
                        complaint.getMessage(),
                        complaint.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ============================== POST ============================== //

    @PostMapping("/account-requests/{error-id}/decision")
    public ResponseEntity<?> decideAccountCreation(@RequestHeader("Authorization") String authHeader,
                                                   @PathVariable("error-id") Long errorId,
                                                   @RequestBody Map<String, String> requestBody) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(authResult.getStatusCode()).body(null);

        Optional<ProfileError> profileErrorOptional = profileErrorRepository.findById(errorId);
        if (profileErrorOptional.isEmpty())
            return ResponseEntity.status(404).body("No ProfileError found with specified ID.");

        ProfileError profileError = profileErrorOptional.get();
        String decision = requestBody.get("decision");

        if ("denied".equalsIgnoreCase(decision)) {
            profileErrorRepository.delete(profileError);
            return ResponseEntity.ok("Account creation request denied. ProfileError deleted.");
        } else if ("approved".equalsIgnoreCase(decision)) {
            RegisterRequest registerRequest = new RegisterRequest(
                    profileError.getPib(),
                    profileError.getEmail(),
                    profileError.getPassword(),
                    profileError.getPhoneNumber(),
                    profileError.getRole(),
                    null,
                    profileError.getUserDescription(),
                    true
            );
            profileErrorRepository.delete(profileError);
            return authController.register(registerRequest);
        } else {
            return ResponseEntity.status(400).body("Invalid decision value. Must be 'approved' or 'denied'.");
        }
    }

    @PostMapping("/complaints/{complaint-id}/status")
    public ResponseEntity<?> updateComplaintStatus(@PathVariable("complaint-id") Long complaintId,
                                                   @RequestHeader("Authorization") String authHeader,
                                                   @RequestBody Map<String, String> requestBody) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(authResult.getStatusCode()).body(null);

        Optional<Complaint> complaintOptional = complaintRepository.findById(complaintId);
        if (complaintOptional.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No complaint with specified id found."));
        Complaint complaint = complaintOptional.get();
        User authenticatedUser = (User) authResult.getBody();

        if (!authenticatedUser.getId().equals(complaint.getModerator().getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not authorized to change the status of this complaint."));

        String newStatus = requestBody.get("status");
        if (newStatus == null || newStatus.trim().isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Status value is required."));
        complaint.setStatus(newStatus);
        complaintRepository.save(complaint);
        return ResponseEntity.ok(Map.of("message", "Complaint status updated successfully."));
    }

    // ============================== DELETE ============================== //

    @DeleteMapping("/complaints/{complaint-id}")
    public ResponseEntity<?> deleteComplaint(@PathVariable("complaint-id") Long complaintId,
                                             @RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(authResult.getStatusCode()).body(null);

        Optional<Complaint> complaintOptional = complaintRepository.findById(complaintId);
        if (complaintOptional.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No complaint with specified id found."));

        Complaint complaint = complaintOptional.get();
        User authenticatedUser = (User) authResult.getBody();

        if (!authenticatedUser.equals(complaint.getModerator()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not authorized to delete this complaint."));
        complaintRepository.delete(complaint);
        return ResponseEntity.ok(Map.of("message", "Complaint deleted successfully."));
    }

    @DeleteMapping("/profile-errors/{error-id}")
    public ResponseEntity<?> deleteProfileError(@PathVariable("error-id") Long errorId,
                                                @RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(authResult.getStatusCode()).body(null);

        Optional<ProfileError> profileErrorOptional = profileErrorRepository.findById(errorId);
        if (profileErrorOptional.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No profile error with specified id found."));
        ProfileError profileError = profileErrorOptional.get();

        profileErrorRepository.delete(profileError);
        return ResponseEntity.ok(Map.of("message", "Profile error deleted successfully."));
    }

    @DeleteMapping("/post-errors/{error-id}")
    public ResponseEntity<?> deletePostError(@PathVariable("error-id") Long errorId,
                                             @RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(authResult.getStatusCode()).body(null);

        Optional<PostError> postErrorOptional = postErrorRepository.findById(errorId);
        if (postErrorOptional.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No post error with specified id found."));
        PostError postError = postErrorOptional.get();
        postErrorRepository.delete(postError);
        return ResponseEntity.ok(Map.of("message", "Post error deleted successfully."));
    }
}
