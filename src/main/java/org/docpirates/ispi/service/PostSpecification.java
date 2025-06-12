package org.docpirates.ispi.service;

import org.docpirates.ispi.entity.Post;
import org.docpirates.ispi.enums.PostStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class PostSpecification {

    public static Specification<Post> isOpen() {
        return (root, query, cb) -> cb.equal(root.get("status"), PostStatus.OPEN);
    }

    public static Specification<Post> hasUniversity(String university) {
        return (root, query, cb) -> cb.equal(root.get("institution").get("name"), university);
    }

    public static Specification<Post> hasSubjectArea(String subjectArea) {
        return (root, query, cb) -> cb.equal(root.get("subjectArea").get("name"), subjectArea);
    }

    public static Specification<Post> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> cb.between(root.get("initialPrice"), min, max);
    }

}
