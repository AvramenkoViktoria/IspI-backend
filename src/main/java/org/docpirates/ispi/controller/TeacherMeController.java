package org.docpirates.ispi.controller;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.ResponseChainDto;
import org.docpirates.ispi.entity.*;
import org.docpirates.ispi.enums.RespondentType;
import org.docpirates.ispi.repository.DealRepository;
import org.docpirates.ispi.repository.PostRepository;
import org.docpirates.ispi.repository.ResponseRepository;
import org.docpirates.ispi.repository.TeacherRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teachers/me")
@RequiredArgsConstructor
public class TeacherMeController {

    private final TeacherRepository teacherRepository;
    private final DealRepository dealRepository;
    private final UserMeController userMeController;
    private final ResponseRepository responseRepository;
    private final PostRepository postRepository;

    // ============================== GET ============================== //

    @GetMapping("/{teacherId}/rating")
    public ResponseEntity<?> getTeacherRating(@PathVariable Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        if (teacher == null)
            return ResponseEntity.status(404).body("{\"message\": \"No teacher with specified id found.\"}");

        List<Deal> ratedDeals = dealRepository.findAllByTeacherIdAndStudentFeedbackGreaterThan(teacherId, 0);
        if (ratedDeals.isEmpty())
            return ResponseEntity.ok(Map.of("rating", 0.0));

        double averageRating = ratedDeals.stream()
                .mapToDouble(Deal::getStudentFeedback)
                .average()
                .orElse(0.0);
        return ResponseEntity.ok(Map.of("rating", averageRating));
    }

    @GetMapping("/responses")
    public ResponseEntity<?> getResponseChainsForTeacher(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;

        User user = (User) authResult.getBody();
        if (!(user instanceof Teacher teacher))
            return ResponseEntity.status(403).body("{\"message\": \"Only teachers can access this endpoint.\"}");

        List<Response> teacherResponses = responseRepository.findAllByRespondentId(teacher.getId());
        if (teacherResponses.isEmpty())
            return ResponseEntity.status(204).body("{\"message\": \"No responses for this user found.\"}");

        Set<Long> postIds = teacherResponses.stream()
                .map(response -> response.getPost().getId())
                .collect(Collectors.toSet());

        List<Response> allResponsesOnPosts = responseRepository.findAllByPostIdIn(postIds);
        if (allResponsesOnPosts.isEmpty())
            return ResponseEntity.status(204).body("{\"message\": \"No responses for this user found.\"}");

        Map<Long, Response> prevMap = allResponsesOnPosts.stream()
                .filter(r -> r.getPrevResponseId() != null)
                .collect(Collectors.toMap(Response::getPrevResponseId, r -> r));

        List<List<ResponseChainDto>> chains = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        for (Response r : allResponsesOnPosts) {
            if (r.getPrevResponseId() != null) continue;
            List<ResponseChainDto> chain = new ArrayList<>();
            Response current = r;
            boolean teacherParticipates = false;
            while (current != null && !visited.contains(current.getId())) {
                chain.add(ResponseChainDto.fromEntity(current));
                visited.add(current.getId());
                if (current.getRespondent().getId().equals(teacher.getId()))
                    teacherParticipates = true;
                current = prevMap.get(current.getId());
            }
            if (teacherParticipates)
                chains.add(chain);
        }

        if (chains.isEmpty())
            return ResponseEntity.status(204).body("{\"message\": \"No responses for this user found.\"}");
        return ResponseEntity.ok(chains);
    }

    // ============================== POST ============================== //

    @PostMapping("/posts/{postId}/responses")
    public ResponseEntity<?> createResponse(@RequestHeader("Authorization") String authHeader,
                                            @PathVariable Long postId,
                                            @RequestBody Map<String, Object> payload) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        if (!(user instanceof Teacher))
            return ResponseEntity.status(403).body("{\"message\": \"Only teachers can respond to posts.\"}");

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.status(404).body("{\"message\": \"No post with specified id found.\"}");

        BigDecimal price;
        try {
            price = new BigDecimal(payload.get("price").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"message\": \"Invalid or missing 'price'.\"}");
        }
        Response response = Response.builder()
                .creationDate(LocalDateTime.now())
                .price(price)
                .prevResponseId(null)
                .respondent(user)
                .respondentType(RespondentType.TEACHER)
                .post(post)
                .build();

        responseRepository.save(response);
        return ResponseEntity.ok().body("{\"message\": \"Response successfully created.\"}");
    }

    // ============================== DELETE ============================== //

    @DeleteMapping("/posts/{postId}/responses")
    public ResponseEntity<?> deleteMyResponses(@RequestHeader("Authorization") String authHeader,
                                               @PathVariable Long postId) {
        ResponseEntity<?> authResult = userMeController.authenticateUser(authHeader);
        if (!authResult.getStatusCode().is2xxSuccessful())
            return authResult;
        User user = (User) authResult.getBody();

        if (!(user instanceof Teacher))
            return ResponseEntity.status(403).body("{\"message\": \"Only teachers can delete their responses.\"}");

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null)
            return ResponseEntity.status(404).body("{\"message\": \"No post with specified id found.\"}");

        List<Response> responses = responseRepository.findAllByPostIdAndRespondentId(postId, user.getId());
        if (!responses.isEmpty())
            responseRepository.deleteAll(responses);
        return ResponseEntity.ok().body("{\"message\": \"Responses successfully deleted.\"}");
    }

}
