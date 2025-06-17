package org.docpirates.ispi.controller;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.PostDto;
import org.docpirates.ispi.dto.SubscriptionDto;
import org.docpirates.ispi.entity.Post;
import org.docpirates.ispi.entity.Subscription;
import org.docpirates.ispi.repository.PostRepository;
import org.docpirates.ispi.repository.SubscriptionRepository;
import org.docpirates.ispi.service.PostSpecification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ServiceController {

    private final PostRepository postRepository;
    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/posts")
    public ResponseEntity<List<PostDto>> getOpenPosts(
            @RequestParam(required = false) String university,
            @RequestParam(required = false) String subjectArea,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Specification<Post> spec = PostSpecification.isOpen();

        if (university != null)
            spec = spec.and(PostSpecification.hasUniversity(university));
        if (subjectArea != null)
            spec = spec.and(PostSpecification.hasSubjectArea(subjectArea));
        if (priceMin != null && priceMax != null) {
            if (priceMin.compareTo(priceMax) > 0)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            spec = spec.and(PostSpecification.priceBetween(priceMin, priceMax));
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortBy = "price".equalsIgnoreCase(sort) ? "initialPrice" : "creationDate";
        if (offset > limit)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(direction, sortBy));

        return ResponseEntity.ok(postRepository.findAll(spec, pageable)
                .stream()
                .map(PostDto::fromEntity)
                .toList());
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionDto>> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        List<SubscriptionDto> result = subscriptions.stream()
                .map(SubscriptionDto::from)
                .toList();
        return ResponseEntity.ok(result);
    }
}
