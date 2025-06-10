package org.docpirates.ispi.controller;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.*;
import org.docpirates.ispi.entity.*;
import org.docpirates.ispi.enums.DealStatus;
import org.docpirates.ispi.enums.PostStatus;
import org.docpirates.ispi.enums.RespondentType;
import org.docpirates.ispi.repository.*;
import org.docpirates.ispi.service.ContactInfoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students/me")
@RequiredArgsConstructor
public class StudentMeController {

    private final PostErrorRepository postErrorRepository;
    private final InstitutionRepository institutionRepository;
    private final WorkTypeRepository workTypeRepository;
    private final SubjectAreaRepository subjectAreaRepository;
    private final PostRepository postRepository;
    private final UserMeController userMeController;
    private final ResponseRepository responseRepository;
    private final DealRepository dealRepository;

    // ============================== POST ============================== //

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreatePostRequest request) {

        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;

        User user = (User) authResult.getBody();
        if (!(user instanceof Student student))
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Only students can create posts."));

        String description = request.getDescription();
        boolean containsSensitive = ContactInfoService.containsContactInfo(description);

        if (containsSensitive) {
            PostError postError = PostError.builder()
                    .description("Post description contains sensitive data")
                    .creationDate(LocalDateTime.now())
                    .workType(request.getWorkType())
                    .university(request.getUniversity())
                    .subjectArea(request.getSubjectArea())
                    .postDescription(description)
                    .initialPrice(request.getInitialPrice())
                    .status("REJECTED")
                    .student(student)
                    .existingPost(false)
                    .build();
            postErrorRepository.save(postError);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Post description has sensitive data. The post error was created."));
        }

        WorkType workType = workTypeRepository.findByNameIgnoreCase(request.getWorkType()).orElse(null);
        Institution institution = institutionRepository.findByNameIgnoreCase(request.getUniversity()).orElse(null);
        SubjectArea subjectArea = subjectAreaRepository.findByNameIgnoreCase(request.getSubjectArea()).orElse(null);
        if (workType == null || institution == null || subjectArea == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid work type, university, or subject area."));
        }

        Post post = Post.builder()
                .workType(workType)
                .institution(institution)
                .subjectArea(subjectArea)
                .description(description)
                .initialPrice(request.getInitialPrice())
                .status(PostStatus.OPEN)
                .student(student)
                .build();

        postRepository.save(post);
        return ResponseEntity.ok(Map.of("message", "The post was successfully created."));
    }

    @PostMapping("/deals")
    public ResponseEntity<?> createDeal(@RequestHeader("Authorization") String authHeader,
                                        @RequestBody DealRequestDto requestDto) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Response response = responseRepository.findById(requestDto.getResponseId()).orElse(null);
        if (response == null)
            return ResponseEntity.status(404).body("{\"message\": \"No response with specified id found.\"}");

        if (response.getRespondentType() != RespondentType.TEACHER)
            return ResponseEntity.badRequest().body("{\"message\": \"The response doesn’t belong to a teacher.\"}");

        if (!response.getPost().getStudent().getId().equals(user.getId()))
            return ResponseEntity.status(403).body("{\"message\": \"You are not the owner of the post.\"}");

        Deal deal = Deal.builder()
                .price(response.getPrice())
                .status(DealStatus.OPEN)
                .post(response.getPost())
                .teacher((Teacher) response.getRespondent())
                .build();

        dealRepository.save(deal);
        return ResponseEntity.ok().body("{\"message\": \"The deal was successfully created.\"}");
    }

    @PostMapping("/deals/{dealId}/feedback")
    public ResponseEntity<?> leaveFeedback(@RequestHeader("Authorization") String authHeader,
                                           @PathVariable Long dealId,
                                           @RequestBody FeedbackRequestDto feedbackDto) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Deal deal = dealRepository.findById(dealId).orElse(null);
        if (deal == null)
            return ResponseEntity.status(404).body("{\"message\": \"No deal with specified id found.\"}");

        if (!deal.getPost().getStudent().getId().equals(user.getId()))
            return ResponseEntity.status(403).body("{\"message\": \"You are not the owner of the deal.\"}");

        if (deal.getStatus() != DealStatus.OPEN)
            return ResponseEntity.badRequest().body("{\"message\": \"The deal is finished.\"}");

        deal.setStudentFeedback(feedbackDto.getFeedback());
        dealRepository.save(deal);
        return ResponseEntity.ok().body("{\"message\": \"The feedback was successfully created.\"}");
    }

    // ============================== PATCH ============================== //

    @PatchMapping("/posts/{postId}/price")
    public ResponseEntity<?> raisePrice(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable Long postId,
                                        @RequestBody PriceUpdateDto priceDto) {

        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.status(404).body("{\"message\": \"No post with specified id found.\"}");

        if (!post.getStudent().getId().equals(user.getId()))
            return ResponseEntity.status(403).body("{\"message\": \"You are not the owner of the post.\"}");

        if (priceDto.getNewPrice().compareTo(post.getInitialPrice()) < 0)
            return ResponseEntity.badRequest().body("{\"message\": \"New price can’t be lower than previous.\"}");

        post.setInitialPrice(priceDto.getNewPrice());
        postRepository.save(post);

        return ResponseEntity.ok().body("{\"message\": \"The price was successfully raised.\"}");
    }

    @PatchMapping("/posts/{postId}")
    public ResponseEntity<?> editPost(@RequestHeader("Authorization") String authHeader,
                                      @PathVariable Long postId,
                                      @RequestBody PostEditDto editDto) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.status(404).body("{\"message\": \"No post with specified id found.\"}");

        if (!post.getStudent().getId().equals(user.getId()))
            return ResponseEntity.status(403).body("{\"message\": \"You are not the owner of the post.\"}");

        if (!editDto.isModeratorFlag() && ContactInfoService.containsContactInfo(editDto.getNewDescription())) {
            PostError error = PostError.builder()
                    .description("Sensitive data found in post description.")
                    .creationDate(LocalDateTime.now())
                    .workType(post.getWorkType().getName())
                    .university(post.getInstitution().getName())
                    .subjectArea(post.getSubjectArea().getName())
                    .postDescription(post.getDescription())
                    .initialPrice(post.getInitialPrice())
                    .status(post.getStatus().name())
                    .student(post.getStudent())
                    .existingPost(true)
                    .build();
            postErrorRepository.save(error);
            return ResponseEntity.badRequest()
                    .body("{\"message\": \"Post description has sensitive data. The post error was created.\"}");
        }

        if (editDto.getNewDescription() != null)
            post.setDescription(editDto.getNewDescription());

        if (editDto.getNewUniversity() != null) {
            Institution newInst = institutionRepository.findByName(editDto.getNewUniversity()).orElse(null);
            if (newInst != null)
                post.setInstitution(newInst);
        }
        postRepository.save(post);
        return ResponseEntity.ok().body("{\"message\": \"The post was successfully changed.\"}");
    }

    @PatchMapping("/deals/{dealId}/finish")
    public ResponseEntity<?> finishDeal(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable Long dealId) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Deal deal = dealRepository.findById(dealId).orElse(null);
        if (deal == null)
            return ResponseEntity.status(404).body("{\"message\": \"No deal with specified id found.\"}");

        if (!deal.getPost().getStudent().getId().equals(user.getId()))
            return ResponseEntity.status(401).body("{\"message\": \"You are not the participant of the deal.\"}");

        deal.setStatus(DealStatus.FINISHED);
        dealRepository.save(deal);
        return ResponseEntity.ok().body("{\"message\": \"The deal was successfully deleted.\"}");
    }

    // ============================== GET ============================== //

    @GetMapping("/posts/{postId}/responses")
    public ResponseEntity<?> getResponseChains(@RequestHeader("Authorization") String authHeader,
                                               @PathVariable Long postId) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.status(404).body("{\"message\": \"No post with specified id found.\"}");

        if (!post.getStudent().getId().equals(user.getId()))
            return ResponseEntity.status(401).body("{\"message\": \"You are not the owner of the post.\"}");

        List<Response> responses = responseRepository.findAllByPostId(postId);
        if (responses.isEmpty())
            return ResponseEntity.status(204).body("{\"message\": \"No responses matching this post id found.\"}");

        Map<Long, Response> prevMap = responses.stream()
                .filter(r -> r.getPrevResponseId() != null)
                .collect(Collectors.toMap(Response::getPrevResponseId, r -> r));
        List<List<ResponseChainDto>> chains = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        for (Response r : responses) {
            if (r.getPrevResponseId() != null) continue;

            List<ResponseChainDto> chain = new ArrayList<>();
            Response current = r;

            while (current != null && !visited.contains(current.getId())) {
                chain.add(ResponseChainDto.fromEntity(current));
                visited.add(current.getId());
                current = prevMap.get(current.getId());
            }

            chains.add(chain);
        }
        return ResponseEntity.ok(chains);
    }

    @GetMapping("/posts")
    public ResponseEntity<?> getMyPosts(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        List<Post> posts = postRepository.findAllByStudentId(user.getId());
        if (posts.isEmpty())
            return ResponseEntity.status(204).body("{\"message\": \"No posts found.\"}");
        List<PostDto> dtos = posts.stream()
                .map(PostDto::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
