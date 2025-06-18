package org.docpirates.ispi.service;


import org.docpirates.ispi.entity.Document;
import org.springframework.data.jpa.domain.Specification;

public class DocumentSpecification {

    public static Specification<Document> hasWorkType(String workType) {
        return (root, query, cb) -> cb.equal(root.get("workType"), workType);
    }

    public static Specification<Document> hasSubjectArea(String subjectArea) {
        return (root, query, cb) -> cb.equal(root.get("subjectArea"), subjectArea);
    }

    public static Specification<Document> hasExtension(String extension) {
        return (root, query, cb) -> cb.equal(root.get("extension"), extension);
    }
}

