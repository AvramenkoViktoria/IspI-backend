package org.docpirates.ispi.controller;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.*;
import org.docpirates.ispi.entity.*;
import org.docpirates.ispi.enums.DealStatus;
import org.docpirates.ispi.repository.*;
import org.docpirates.ispi.service.JwtUtil;
import org.docpirates.ispi.service.SubscriptionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserMeController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final DocumentRepository documentRepository;
    private final UserFavoritesRepository userFavoritesRepository;
    private final DealRepository dealRepository;
    private final PostRepository postRepository;
    private final ComplaintRepository complaintRepository;
    private final ResponseRepository responseRepository;

    public ResponseEntity<?> authenticateUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
        String token = authHeader.substring(7);
        String email;
        try {
            email = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        return ResponseEntity.ok(user);
    }

    // ============================== GET ============================== //

    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        UserDto dto = new UserDto(
                user.getId(),
                user.getPib(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getSubscription() != null ? user.getSubscription().getName() : null,
                user.getLastActivationDate(),
                SubscriptionService.getNextPaymentDate(user.getId(), user.getLastActivationDate())
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/role")
    public ResponseEntity<?> getMyRole(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        String role = switch (user) {
            case Moderator ignored -> "MODERATOR";
            case Teacher ignored -> "TEACHER";
            case Student ignored -> "STUDENT";
            case null, default -> throw new UsernameNotFoundException("Неможливо визначити роль користувача: " + user.getEmail());
        };
        return ResponseEntity.ok(Map.of("role", role));
    }

    @GetMapping("/subscription")
    public ResponseEntity<?> getMySubscription(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Subscription sub = user.getSubscription();
        if (sub == null)
            return ResponseEntity.noContent().build(); // 204
        LocalDateTime last = user.getLastActivationDate();
        LocalDateTime next = SubscriptionService.getNextPaymentDate(user.getId(), last);
        MySubscriptionDto dto = new MySubscriptionDto(
                sub.getId(),
                sub.getName(),
                sub.getDescription(),
                sub.getPrice(),
                last,
                next
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/documents")
    public ResponseEntity<?> countMyDocuments(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sinceDate) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        if (sinceDate != null) {
            int count = subscriptionService.getNumberOfUploadedDocuments(sinceDate.atStartOfDay(), user);
            return ResponseEntity.ok(Map.of("number", count));
        }
        return ResponseEntity.ok(subscriptionService.getDocumentsByAuthor(user));
    }

    @GetMapping("/users/me/documents/{documentId}")
    public ResponseEntity<?> downloadDocument(
            @PathVariable Long documentId,
            @RequestHeader("Authorization") String authHeader
    ) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Optional<Document> optionalDoc = documentRepository.findById(documentId);
        if (optionalDoc.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No document with specified id found."));

        Document doc = optionalDoc.get();
        if (!doc.getAuthor().getId().equals(user.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not allowed to access this document."));

        File file = new File(doc.getDiskPath());
        if (!file.exists())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "File not found on server."));

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String mimeType = DocumentDto.from(doc).content_type();
            String fileName = doc.getName() + "." + doc.getExtension();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(fileBytes);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error reading the file."));
        }
    }

    @GetMapping("/users/me/favorites")
    public ResponseEntity<?> getUserFavoriteDocuments(
            @RequestHeader("Authorization") String authHeader
    ) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        List<UserFavorites> favorites = userFavoritesRepository.findByUserId(user.getId());
        if (favorites.isEmpty())
            return ResponseEntity.noContent().build(); // 204 No Content
        List<DocumentDto> result = favorites.stream()
                .map(UserFavorites::getDocument)
                .map(DocumentDto::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionDto>> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        List<SubscriptionDto> result = subscriptions.stream()
                .map(SubscriptionDto::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/me/deals")
    public ResponseEntity<?> getUserDeals(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        switch (user) {
            case Teacher ignored -> {
                return ResponseEntity.ok(dealRepository.findByTeacher((Teacher) user));
            }
            case Student ignored -> {
                List<Post> myPosts = postRepository.findByStudent((Student) user);
                return ResponseEntity.ok(dealRepository.findByPostIn(myPosts));
            }
            default -> {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
        }
    }

    @GetMapping("/deals/{dealId}/complaints")
    public ResponseEntity<?> getMyComplaint(
            @PathVariable("dealId") Long dealId,
            @RequestHeader("Authorization") String authHeader
    ) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Optional<Deal> dealOpt = dealRepository.findById(dealId);
        if (dealOpt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("message", "No deal with specified id found."));

        Optional<Complaint> complaintOpt = complaintRepository.findByDeal(dealOpt.get());
        if (complaintOpt.isEmpty())
            return ResponseEntity.noContent().build();

        Complaint complaint = complaintOpt.get();
        if (!complaint.getPlaintiff().getId().equals(user.getId()))
            return ResponseEntity.noContent().build();

        MyComplaintDto dto = MyComplaintDto.builder()
                .complaintId(complaint.getId())
                .message(complaint.getDescription())
                .build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/posts/{postId}/last-student-response/{teacherId}")
    public ResponseEntity<?> getLastStudentResponse(
            @PathVariable Long postId,
            @PathVariable Long teacherId,
            @RequestHeader("Authorization") String authHeader
    ) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No teacher/post with specified id found."));

        Optional<Response> responseOpt = responseRepository.findNewestStudentResponseByPostIdAndTeacherResponse(postId, teacherId);
        if (responseOpt.isEmpty())
            return ResponseEntity.noContent().build();

        Response response = responseOpt.get();

        if (!response.getRespondent().getId().equals(user.getId()))
            return ResponseEntity.noContent().build();

        LastStudentResponseDto dto = LastStudentResponseDto.builder()
                .response_id(response.getId())
                .price(response.getPrice())
                .creation_date(response.getCreationDate())
                .build();

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/posts/{postId}/last-teacher-response/{teacherId}")
    public ResponseEntity<?> getLastTeacherResponse(
            @PathVariable Long postId,
            @PathVariable Long teacherId,
            @RequestHeader("Authorization") String authHeader
    ) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No teacher/post with specified id found."));

        Optional<Response> responseOpt = responseRepository.findTopByPostIdAndRespondentIdOrderByCreationDateDesc(postId, teacherId);
        if (responseOpt.isEmpty())
            return ResponseEntity.noContent().build();

        Response response = responseOpt.get();

        LastTeacherResponseDto dto = LastTeacherResponseDto.builder()
                .response_id(response.getId())
                .price(response.getPrice())
                .creation_date(response.getCreationDate())
                .build();
        return ResponseEntity.ok(dto);
    }

    // ============================== DELETE ============================== //

    @DeleteMapping("/api/users/me/favorites/{fileId}")
    public ResponseEntity<?> removeFromFavorites(
            @PathVariable("fileId") Long fileId,
            @RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Optional<UserFavorites> favoriteOpt = userFavoritesRepository.findByUserIdAndDocumentId(user.getId(), fileId);
        if (favoriteOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No file with specified id found."));
        userFavoritesRepository.deleteByUserIdAndDocumentId(user.getId(), fileId);
        return ResponseEntity.ok(Map.of("message", "File was successfully deleted."));
    }

    @DeleteMapping("/api/users/me/subscription")
    public ResponseEntity<?> removeMySubscription(
            @RequestHeader("Authorization") String authHeader) {

        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;

        User user = (User) authResult.getBody();
        user.setSubscription(null);
        user.setLastActivationDate(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Subscription was successfully deleted."));
    }

    // ============================== POST ============================== //

    @PostMapping("/api/users/me/favorites/{fileId}")
    public ResponseEntity<?> addFileToFavorites(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("fileId") Long fileId) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Document document = documentRepository.findById(fileId).orElse(null);
        if (document == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No document with specified id found."));
        boolean alreadyExists = userFavoritesRepository.existsByUserAndDocument(user, document);
        if (alreadyExists)
            return ResponseEntity.ok(Map.of("message", "File is already in favorites."));

        UserFavorites favorite = UserFavorites.builder()
                .user(user)
                .document(document)
                .build();
        userFavoritesRepository.save(favorite);
        return ResponseEntity.ok(Map.of("message", "File was successfully added to favorites."));
    }

    @PostMapping("/api/users/me/subscription/{subscriptionId}")
    public ResponseEntity<?> subscribeToPlan(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("subscriptionId") Long subscriptionId) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No subscription with specified id found."));
        user.setSubscription(subscription);
        user.setLastActivationDate(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "The subscription was successfully completed."));
    }

    @PostMapping("/api/users/me/deals/{dealId}/complaint")
    public ResponseEntity<?> createComplaint(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("dealId") Long dealId,
            @RequestBody Map<String, String> requestBody) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Deal deal = dealRepository.findById(dealId).orElse(null);
        if (deal == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No deal with specified id found."));

        Complaint complaint = Complaint.builder()
                .description(requestBody.get("message"))
                .creationDate(LocalDateTime.now())
                .status("NEW")
                .plaintiff(user)
                .deal(deal)
                .build();
        complaintRepository.save(complaint);
        return ResponseEntity.ok(Map.of("message", "The complaint was successfully created"));
    }

    @PostMapping("/api/users/me/deals/{dealId}/feedback")
    public ResponseEntity<?> leaveDealFeedback(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("dealId") Long dealId,
            @RequestBody Map<String, String> requestBody) {

        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Deal deal = dealRepository.findById(dealId).orElse(null);
        if (deal == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No deal with specified id found."));

        if (!deal.getPost().getStudent().getId().equals(user.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not allowed to leave feedback on this deal."));

        if (deal.getStatus() != DealStatus.OPEN)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "You can leave feedback only on open deals."));

        int stars;
        try {
            stars = Integer.parseInt(requestBody.get("stars"));
            if (stars < 1 || stars > 5) {
                return ResponseEntity.badRequest().body(Map.of("message", "Stars must be between 1 and 5."));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid stars value."));
        }

        deal.setStudentFeedback(stars);
        dealRepository.save(deal);
        return ResponseEntity.ok(Map.of("message", "The feedback was successfully created."));
    }

    // ============================== PATCH ============================== //

    @PatchMapping("/api/users/me/subscription/{subscriptionId}")
    public ResponseEntity<?> changeMySubscription(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("subscriptionId") Long subscriptionId) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No subscription with specified id found."));

        user.setSubscription(subscription);
        user.setLastActivationDate(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "The subscription was successfully changed."));
    }

}

