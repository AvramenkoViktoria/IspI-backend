package org.docpirates.ispi.controller;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.*;
import org.docpirates.ispi.entity.*;
import org.docpirates.ispi.enums.ContactErrorStatus;
import org.docpirates.ispi.enums.RespondentType;
import org.docpirates.ispi.repository.*;
import org.docpirates.ispi.service.ContactInfoService;
import org.docpirates.ispi.service.JwtUtil;
import org.docpirates.ispi.service.SubscriptionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final DocumentFeedbackRepository documentFeedbackRepository;
    private final DocumentComplaintRepository documentComplaintRepository;
    private final UserDownloadsRepository userDownloadsRepository;
    private final ProfileErrorRepository profileErrorRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

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
            case null, default ->
                    throw new UsernameNotFoundException("Неможливо визначити роль користувача: " + user.getEmail());
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

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<?> downloadDocument(
            @PathVariable Long documentId,
            @RequestHeader("Authorization") String authHeader
    ) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;

        User user = (User) authResult.getBody();

        Subscription sub = user.getSubscription();
        if (sub == null || Objects.requireNonNull(
                        SubscriptionService.getNextPaymentDate(sub.getId(), user.getLastActivationDate()))
                .isBefore(LocalDate.now().plusDays(1).atStartOfDay())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not allowed to download documents."));
        }

        Optional<Subscription> patron = subscriptionRepository.findByNameIgnoreCase("patron");
        if (patron.isPresent() && Objects.equals(sub.getId(), patron.get().getId())) {
            int numOfDownloads = userDownloadsRepository.countDownloadsSince(user, LocalDateTime.now().minusDays(1));
            if (numOfDownloads >= 10)
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You have reached the limit of 10 documents per day for patron subscription."));
        }

        Optional<Document> optionalDoc = documentRepository.findById(documentId);
        if (optionalDoc.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No document with specified id found."));

        Document doc = optionalDoc.get();
        File file = new File(doc.getDiskPath());
        if (!file.exists())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "File not found on server."));

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String mimeType = DocumentDto.from(doc).content_type();
            String fileName = doc.getName() + "." + doc.getExtension();

            UserDownloads download = UserDownloads.builder()
                    .user(user)
                    .document(doc)
                    .build();
            userDownloadsRepository.save(download);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(fileBytes);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error reading the file."));
        }
    }


    @GetMapping("/favorites")
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

    @GetMapping("/deals")
    public ResponseEntity<?> getUserDeals(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();
        List<Deal> deals;

        if (user instanceof Teacher teacher) {
            deals = dealRepository.findByTeacher(teacher);
        } else if (user instanceof Student student) {
            List<Post> myPosts = postRepository.findByStudent(student);
            deals = dealRepository.findByPostIn(myPosts);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"message\": \"Invalid token\"}");
        }

        if (deals.isEmpty())
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body("{\"message\": \"No deals found for this user.\"}");
        List<DealPostDto> dtos = deals.stream()
                .map(DealPostDto::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/deals/{dealId}/complaints")
    public ResponseEntity<?> getMyComplaints(
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

        List<Complaint> complaints = complaintRepository.findByDeal(dealOpt.get());
        if (complaints.isEmpty())
            return ResponseEntity.noContent().build();

        return complaints.stream()
                .filter(c -> c.getPlaintiff().getId().equals(user.getId()))
                .findFirst()
                .map(complaint -> {
                    MyComplaintDto dto = MyComplaintDto.builder()
                            .complaintId(complaint.getId())
                            .message(complaint.getDescription())
                            .build();
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.noContent().build());
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

        Optional<Response> responseOpt = responseRepository.findNewestStudentResponseByPostIdAndTeacherResponse(postId, teacherId, RespondentType.STUDENT);
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

    @DeleteMapping("/favorites/{fileId}")
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

    @DeleteMapping("/subscription")
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

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable Long postId) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;

        User user = (User) authResult.getBody();
        boolean isModerator = user instanceof Moderator;
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.status(404).body("{\"message\": \"No post with specified id found.\"}");
        boolean isOwner = post.getStudent().getId().equals(user.getId());

        Deal deal = dealRepository.findByPostId(postId).orElse(null);
        if (!isOwner && !isModerator)
            return ResponseEntity.status(403).body("{\"message\": \"You are not allowed to delete this post.\"}");
        if (!isModerator && deal != null)
            return ResponseEntity.status(400).body("{\"message\": \"Post is already involved in a deal and cannot be deleted.\"}");

        responseRepository.deleteAllByPostId(postId);
        if (isModerator && deal != null) {
            complaintRepository.deleteAllByDealId(deal.getId());
            dealRepository.delete(deal);
        }
        postRepository.delete(post);
        return ResponseEntity.ok("{\"message\": \"Post was successfully deleted.\"}");
    }

    // ============================== POST ============================== //

    @PostMapping("/favorites/{fileId}")
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

    @PostMapping("/subscription/{subscriptionId}")
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

    @PostMapping("/deals/{dealId}/complaint")
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
                .status("REGULAR")
                .plaintiff(user)
                .deal(deal)
                .build();
        complaintRepository.save(complaint);
        return ResponseEntity.ok(Map.of("message", "The complaint was successfully created"));
    }

    @PostMapping("/{documentId}/feedback")
    public ResponseEntity<?> addFeedback(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("documentId") Long documentId,
            @RequestBody DocumentFeedbackRequest request) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        if (request.stars() < 1 || request.stars() > 5)
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Stars must be between 1 and 5."));

        Optional<Document> optionalDocument = documentRepository.findById(documentId);
        if (optionalDocument.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No document with specified id found."));

        Document document = optionalDocument.get();
        if (documentFeedbackRepository.existsByUserAndDocument(user, document))
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "You have already submitted feedback for this document."));

        DocumentFeedback feedback = DocumentFeedback.builder()
                .document(document)
                .user(user)
                .stars(request.stars())
                .build();
        documentFeedbackRepository.save(feedback);
        return ResponseEntity.ok(Map.of("message", "The feedback was successfully created."));
    }

    @PostMapping("/responses/{responseId}")
    public ResponseEntity<?> respondToResponse(@RequestHeader("Authorization") String authHeader,
                                               @PathVariable Long responseId,
                                               @RequestBody Map<String, BigDecimal> body) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;

        User user = (User) authResult.getBody();
        BigDecimal price = body.get("price");

        Response baseResponse = responseRepository.findById(responseId).orElse(null);
        if (baseResponse == null)
            return ResponseEntity.status(404).body(Map.of("message", "No response with specified id found."));

        Post post = baseResponse.getPost();
        List<Response> responses = responseRepository.findAllByPostId(post.getId());
        Map<Long, Response> responseMap = responses.stream()
                .collect(Collectors.toMap(Response::getId, r -> r));

        List<Response> chain = new ArrayList<>();
        Response current = baseResponse;
        while (current != null) {
            chain.add(0, current);
            current = responseMap.get(current.getPrevResponseId());
        }
        Response allowedTarget = null;

        if (user instanceof Teacher) {
            List<Response> candidates = chain.stream()
                    .filter(r -> r.getRespondent().getId().equals(user.getId())
                                 || r.getRespondent() instanceof Student)
                    .sorted(Comparator.comparing(Response::getCreationDate).reversed())
                    .toList();
            allowedTarget = candidates.isEmpty() ? null : candidates.get(0);
        } else if (user instanceof Student) {
            List<Response> candidates = chain.stream()
                    .filter(r -> r.getRespondent().getId().equals(user.getId())
                                 || r.getRespondent() instanceof Teacher)
                    .sorted(Comparator.comparing(Response::getCreationDate).reversed())
                    .toList();
            allowedTarget = candidates.isEmpty() ? null : candidates.get(0);
        } else {
            return ResponseEntity.status(401).body(Map.of("message", "Only students and teachers can respond."));
        }

        if (!allowedTarget.getId().equals(responseId))
            return ResponseEntity.status(400).body(Map.of("message", "You can only respond to the latest allowed response in the chain."));

        Response newResponse = Response.builder()
                .creationDate(LocalDateTime.now())
                .price(price)
                .respondent(user)
                .respondentType(user instanceof Teacher ? RespondentType.TEACHER : RespondentType.STUDENT)
                .post(post)
                .prevResponseId(allowedTarget.getId())
                .build();

        responseRepository.save(newResponse);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/documents/{document-id}/complaint")
    public ResponseEntity<?> fileComplaint(@PathVariable("document-id") Long documentId,
                                           @RequestHeader("Authorization") String authHeader,
                                           @RequestBody Map<String, String> requestBody) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;

        Optional<Document> documentOptional = documentRepository.findById(documentId);
        if (documentOptional.isEmpty())
            return ResponseEntity.status(404).body(Map.of("message", "No document with specified id found."));

        User plaintiff = (User) authResult.getBody();
        DocumentComplaint complaint = DocumentComplaint.builder()
                .document(documentOptional.get())
                .plaintiff(plaintiff)
                .message(requestBody.get("message"))
                .createdAt(LocalDateTime.now())
                .build();
        documentComplaintRepository.save(complaint);
        return ResponseEntity.ok(Map.of("message", "The complaint was successfully created"));
    }

    @PostMapping("/activate-subscription")
    public ResponseEntity<?> activateSubscription(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Subscription subscription = user.getSubscription();
        if (subscription == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "User is not subscribed to any plan."));

        LocalDate today = LocalDate.now();
        user.setLastActivationDate(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Subscription activated successfully.",
                "activatedAt", today,
                "nextPaymentAt", SubscriptionService.getNextPaymentDate(user.getSubscription().getId(), user.getLastActivationDate()),
                "subscriptionType", subscription.getName()
        ));
    }

    // ============================== PATCH ============================== //

    @PatchMapping("/subscription/{subscriptionId}")
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

    @PatchMapping("/edit-profile/{userId}")
    public ResponseEntity<?> editProfile(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long userId,
            @RequestBody RegisterRequest request) {
        ResponseEntity<?> authResponse = authenticateUser(authHeader);
        if (!authResponse.getStatusCode().is2xxSuccessful())
            return authResponse;
        User editor = (User) authResponse.getBody();

        Optional<User> targetOpt = userRepository.findById(userId);
        if (targetOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        User target = targetOpt.get();

        boolean isModerator = editor instanceof Moderator;
        boolean editingOwnProfile = editor.getId().equals(target.getId());

        if (!editingOwnProfile && !isModerator)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to edit this user's profile");

        if (request.email() != null && !request.email().trim().equalsIgnoreCase(target.getEmail())) {
            if (userRepository.existsByEmail(request.email()))
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "This email is already in use"));
            target.setEmail(request.email().trim());
        }

        if (request.phoneNumber() != null && !request.phoneNumber().trim().equals(target.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.phoneNumber()))
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "This phone number is already in use"));
            target.setPhoneNumber(request.phoneNumber().trim());
        }

        if (request.bankCardNumber() != null && !request.bankCardNumber().trim().equals(target.getBankCardNumber())) {
            if (userRepository.existsByBankCardNumber(request.bankCardNumber()))
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "This bank card number is already in use"));
            target.setBankCardNumber(request.bankCardNumber().trim());
        }

        if (request.pib() != null && !request.pib().trim().equalsIgnoreCase(target.getPib())) {
            if (!isModerator && ContactInfoService.containsContactInfo(request.pib())) {
                ProfileError error = ProfileError.builder()
                        .description("PIB contains contact information")
                        .creationDate(LocalDateTime.now())
                        .pib(request.pib())
                        .email(target.getEmail())
                        .phoneNumber(target.getPhoneNumber())
                        .role(target instanceof Teacher ? "TEACHER" : "STUDENT")
                        .userDescription(target instanceof Teacher t ? t.getDescription() : null)
                        .password(target.getPassword())
                        .contactErrorStatus(ContactErrorStatus.REVIEW)
                        .profileId(userId)
                        .build();
                profileErrorRepository.save(error);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("PIB contains contact information. ProfileError was created");
            }
            target.setPib(request.pib().trim());
        }

        if (request.description() != null && target instanceof Teacher teacher) {
            if (!isModerator && ContactInfoService.containsContactInfo(request.description())) {
                ProfileError error = ProfileError.builder()
                        .description("Description contains contact information")
                        .creationDate(LocalDateTime.now())
                        .pib(target.getPib())
                        .email(target.getEmail())
                        .phoneNumber(target.getPhoneNumber())
                        .role("TEACHER")
                        .userDescription(request.description())
                        .password(target.getPassword())
                        .contactErrorStatus(ContactErrorStatus.REVIEW)
                        .profileId(userId)
                        .build();
                profileErrorRepository.save(error);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Description contains contact information. ProfileError was created");
            }
            teacher.setDescription(request.description().trim());
        } else if (request.description() != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only teachers can update description");
        }

        if (request.password() != null)
            target.setPassword(bCryptPasswordEncoder.encode(request.password().trim()));

        userRepository.save(target);
        return ResponseEntity.ok(Map.of("message", "Profile successfully updated"));
    }
}
