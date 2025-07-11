package org.docpirates.ispi.controller;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.ComplaintDto;
import org.docpirates.ispi.dto.DocumentComplaintDto;
import org.docpirates.ispi.dto.PostEditDto;
import org.docpirates.ispi.entity.*;
import org.docpirates.ispi.enums.ComplaintStatus;
import org.docpirates.ispi.enums.ContactErrorStatus;
import org.docpirates.ispi.repository.*;
import org.docpirates.ispi.service.JwtUtil;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private final StudentMeController studentMeController;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

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
    public ResponseEntity<List<ComplaintDto>> getFilteredComplaints(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long moderatorId,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) String plaintiff,
            @RequestParam(defaultValue = "desc") String sort // "asc" or "desc"
    ) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(authResult.getStatusCode()).body(null);
        Specification<Complaint> spec = (root, query, cb) -> cb.conjunction();

        if (moderatorId != null)
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("moderator").get("id"), moderatorId));
        else
            spec = spec.and((root, query, cb) ->
                    cb.isNull(root.get("moderator")));

        if (status != null)
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("status")), status.name()));

        Sort.Direction direction = sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortByDate = Sort.by(direction, "creationDate");

        List<Complaint> complaints = complaintRepository.findAll(spec, sortByDate);
        complaints = complaints.stream()
                .filter(c -> {
                    if ("student".equalsIgnoreCase(plaintiff)) {
                        return c.getPlaintiff() instanceof Student;
                    } else if ("professor".equalsIgnoreCase(plaintiff)) {
                        return c.getPlaintiff() instanceof Teacher;
                    }
                    return true;
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

    @PostMapping("/profile-errors/{error-id}/decision")
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
            profileError.setContactErrorStatus(ContactErrorStatus.DENIED);
            profileErrorRepository.save(profileError);
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

            if (profileError.getProfileId() != 0f) {
                return userMeController.editProfile(
                        authHeader,
                        profileError.getProfileId(),
                        registerRequest
                );
            } else {
                return authController.register(registerRequest);
            }
        } else {
            return ResponseEntity.status(400)
                    .body("Invalid decision value. Must be 'approved' or 'denied'.");
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

    // ============================== PATCH ============================== //

    @PatchMapping("/post-errors/{error-id}")
    public ResponseEntity<?> changePostErrorStatus(@PathVariable("error-id") Long errorId,
                                                   @PathVariable("status") String status,
                                                   @RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(authResult.getStatusCode()).body(null);

        Optional<PostError> postErrorOptional = postErrorRepository.findById(errorId);
        if (postErrorOptional.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No profile error with specified id found."));
        PostError postError = postErrorOptional.get();
        if (status.equalsIgnoreCase("approved")) {
            if (postError.isExistingPost()) {
                Post post = postError.getPost();
                PostEditDto editDto = new PostEditDto();
                if (!(postError.getPostDescription() == null || postError.getPostDescription().isEmpty()))
                    editDto.setNewDescription(post.getDescription());
                if (!(postError.getUniversity() == null || postError.getUniversity().isEmpty()))
                    editDto.setNewUniversity(postError.getUniversity());
                editDto.setModeratorFlag(true);
                studentMeController.editPost(authHeader, post.getId(), editDto);
            }
        } else if (status.equalsIgnoreCase("denied")) {
            postError.setContactErrorStatus(ContactErrorStatus.DENIED);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Your status doesn't match expected value (approved or denied)."));
        }
        postErrorRepository.save(postError);
        return ResponseEntity.ok(Map.of("message", "Post error status changed successfully."));
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
