package org.docpirates.ispi.controller;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.DocumentForIndexing;
import org.docpirates.ispi.entity.Document;
import org.docpirates.ispi.entity.ForbiddenDocument;
import org.docpirates.ispi.entity.User;
import org.docpirates.ispi.repository.DocumentRepository;
import org.docpirates.ispi.repository.ForbiddenDocumentRepository;
import org.docpirates.ispi.repository.UserRepository;
import org.docpirates.ispi.service.DocumentIndexService;
import org.docpirates.ispi.service.TextReader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIndexService documentIndexService;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ForbiddenDocumentRepository forbiddenDocumentRepository;
    private final UserMeController userMeController;
    private final static String DOCPATH = "src/main/java/org/docpirates/ispi/service/user_data/test_files/";
    private final static String FORBIDDEN_DOC_PATH = "src/main/java/org/docpirates/ispi/service/user_data/forbidden_files/";
    private final static Set<String> allowedExtensions = Set.of("pdf", "doc", "docx", "ppt", "pptx");

    @GetMapping("/search")
    public ResponseEntity<List<DocumentForIndexing>> searchDocuments(
            @RequestParam String request,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int to) {
        try {
            List<DocumentForIndexing> results = documentIndexService.searchRelevantDocuments(request, from, to);
            if (results.isEmpty())
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/search/{documentId}")
    public ResponseEntity<List<Map<String, Object>>> findSimilarDocuments(
            @PathVariable String documentId) {
        try {
            Optional<Document> document = documentRepository.findById(Long.parseLong(documentId));
            if (document.isEmpty())
                return ResponseEntity.notFound().build();

            List<Map<String, Object>> results = documentIndexService.findSimilarToDocumentByTitle(document.get().getName());
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long documentId) {
        Optional<Document> document = documentRepository.findById(documentId);
        return document.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestHeader("Authorization") String authHeader,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam("name") String name,
                                    @RequestParam("workType") String workType,
                                    @RequestParam("subjectArea") String subjectArea) {
        ResponseEntity<?> authResponse = userMeController.authenticateUser(authHeader);
        if (!authResponse.getStatusCode().is2xxSuccessful())
            return authResponse;

        User author = (User) authResponse.getBody();

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null)
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "File extension is missing."));

        if (!allowedExtensions.contains(extension.toLowerCase()))
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Unsupported file extension: " + extension));

        String fullFileName = name;
        String diskPath = DOCPATH + fullFileName + "." + extension;

        try {
            Path path = Path.of(diskPath);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            String content = TextReader.getTextFromFile(diskPath);

            DocumentForIndexing incoming = new DocumentForIndexing(diskPath, fullFileName, content);
            List<Document> allDocs = documentRepository.findAll();
            for (Document existing : allDocs) {
                String existingContent = TextReader.getTextFromFile(existing.getDiskPath());
                DocumentForIndexing existingDoc = new DocumentForIndexing(existing.getDiskPath(), existing.getName(), existingContent);

                if (documentIndexService.isSimilarityAboveThreshold(incoming, existingDoc, 90.0))
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "A similar document already exists in the system."));
            }

            List<ForbiddenDocument> allForbiddenDocs = forbiddenDocumentRepository.findAll();
            for (ForbiddenDocument forbidden : allForbiddenDocs) {
                String existingContent = TextReader.getTextFromFile(forbidden.getDiskPath());
                DocumentForIndexing existingDoc = new DocumentForIndexing(forbidden.getDiskPath(), forbidden.getName(), existingContent);

                if (documentIndexService.isSimilarityAboveThreshold(incoming, existingDoc, 50.0))
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "It is prohibited to upload this file."));
            }

            Document document = Document.builder()
                    .uploadedAt(LocalDateTime.now())
                    .name(name)
                    .extension(extension)
                    .workType(workType)
                    .subjectArea(subjectArea)
                    .diskPath(diskPath)
                    .author(author)
                    .build();

            Document saved = documentRepository.save(document);
            documentIndexService.addDocument(incoming);
            return ResponseEntity.ok(saved);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to save the file."));
        }
    }

    @PostMapping(path = "/forbidden", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadForbidden(@RequestHeader("Authorization") String authHeader,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam("name") String name,
                                    @RequestParam("workType") String workType,
                                    @RequestParam("subjectArea") String subjectArea) {
        ResponseEntity<?> authResponse = userMeController.authenticateUser(authHeader);
        if (!authResponse.getStatusCode().is2xxSuccessful())
            return authResponse;

        User author = (User) authResponse.getBody();

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null)
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "File extension is missing."));

        if (!allowedExtensions.contains(extension.toLowerCase()))
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Unsupported file extension: " + extension));

        String fullFileName = name;
        String diskPath = DOCPATH + fullFileName + "." + extension;

        try {
            Path path = Path.of(diskPath);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            String content = TextReader.getTextFromFile(diskPath);

            DocumentForIndexing incoming = new DocumentForIndexing(diskPath, fullFileName, content);
            List<ForbiddenDocument> allDocs = forbiddenDocumentRepository.findAll();
            for (ForbiddenDocument existing : allDocs) {
                String existingContent = TextReader.getTextFromFile(existing.getDiskPath());
                DocumentForIndexing existingDoc = new DocumentForIndexing(existing.getDiskPath(), existing.getName(), existingContent);

                if (documentIndexService.isSimilarityAboveThreshold(incoming, existingDoc, 80.0))
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "A similar document already exists in the system."));
            }

            ForbiddenDocument document = ForbiddenDocument.builder()
                    .uploadedAt(LocalDateTime.now())
                    .name(name)
                    .extension(extension)
                    .workType(workType)
                    .subjectArea(subjectArea)
                    .diskPath(diskPath)
                    .author(author)
                    .build();

            ForbiddenDocument saved = forbiddenDocumentRepository.save(document);
            return ResponseEntity.ok(saved);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to save the file."));
        }
    }

}

