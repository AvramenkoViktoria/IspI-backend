package org.docpirates.ispi.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.DocumentForIndexing;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentIndexService {

    private final ElasticsearchClient client;

    public List<DocumentForIndexing> readDocumentsFromDirectory(Path rootDir) throws IOException {
        List<DocumentForIndexing> documents = new ArrayList<>();

        Files.walk(rootDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        String content = TextReader.getTextFromFile(file.toString());
                        documents.add(new DocumentForIndexing(
                                file.toString(),
                                file.getFileName().toString(),
                                content
                        ));
                    } catch (IOException e) {
                        System.err.println("Cannot read file: " + file + " - " + e.getMessage());
                    }
                });

        return documents;
    }

    public void indexDocuments(List<DocumentForIndexing> documents) {
        try {
            if (client.indices().exists(b -> b.index("doc_index")).value()) {
                client.indices().delete(b -> b.index("doc_index"));
                System.out.println("Deleted old index 'doc_index'");
            }

            client.indices().create(b -> b
                    .index("doc_index")
                    .mappings(mb -> mb
                            .properties("path", p -> p.keyword(k -> k))
                            .properties("filename", p -> p.text(t -> t))
                            .properties("content", p -> p.text(t -> t))
                    )
            );
            System.out.println("Created new index 'doc_index'");

        } catch (IOException e) {
            System.err.println("Failed to recreate index: " + e.getMessage());
            return;
        }

        for (DocumentForIndexing doc : documents) {
            try {
                addDocument(doc);
            } catch (IOException e) {
                System.err.println("Failed to index document: " + doc.getFilename() + " - " + e.getMessage());
            }
        }
    }

    public void addDocument(DocumentForIndexing document) throws IOException {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("path", document.getPath());
        jsonMap.put("filename", document.getFilename());
        jsonMap.put("content", document.getContent());

        IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index("doc_index")
                .document(jsonMap)
        );

        IndexResponse response = client.index(request);
        System.out.println("Indexed document ID: " + response.id());
    }

    public int getTotalDocumentCount() throws IOException {
        CountRequest countRequest = CountRequest.of(c -> c.index("doc_index"));
        CountResponse countResponse = client.count(countRequest);
        System.out.println("Total document count: " + countResponse.count());
        return (int) countResponse.count();
    }

    public List<DocumentForIndexing> searchRelevantDocuments(String userQuery, int from, int to) throws IOException {
        int maxDocs = getTotalDocumentCount();
        if (from < 0) from = 0;
        if (to > maxDocs) to = maxDocs;
        if (from >= to) return List.of();
        int size = to - from;

        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(userQuery)
                .fields("filename^3", "content")
        )._toQuery();

        int finalFrom = from;
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index("doc_index")
                .from(finalFrom)
                .size(size)
                .query(multiMatchQuery)
                .sort(srt -> srt
                        .field(f -> f
                                .field("_score")
                                .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                        )
                )
        );

        SearchResponse<Map> searchResponse = client.search(searchRequest, Map.class);

        return searchResponse.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(source -> new DocumentForIndexing(
                        (String) source.get("path"),
                        (String) source.get("filename"),
                        (String) source.get("content")
                ))
                .collect(Collectors.toList());
    }

    public boolean isSimilarityAboveThreshold(DocumentForIndexing doc1,
                                              DocumentForIndexing doc2,
                                              double thresholdPercent) {
        Set<String> terms1 = extractTerms(doc1.getContent());
        Set<String> terms2 = extractTerms(doc2.getContent());
        if (terms1.isEmpty() || terms2.isEmpty()) return false;
        Set<String> intersection = new HashSet<>(terms1);
        intersection.retainAll(terms2);
        double similarity = 100.0 * intersection.size() / Math.min(terms1.size(), terms2.size());
        return similarity >= thresholdPercent;
    }

    private Set<String> extractTerms(String content) {
        return Arrays.stream(content.toLowerCase().split("\\W+"))
                .filter(term -> term.length() > 2)
                .collect(Collectors.toSet());
    }

    public List<Map<String, Object>> findSimilarToDocumentByTitle(String title) throws IOException {
        String index = "doc_index";

        SearchResponse<Map> findDocResponse = client.search(
                s -> s
                        .index(index)
                        .size(1)
                        .query(q -> q
                                .match(m -> m
                                        .field("filename")
                                        .query(title)
                                )
                        ),
                Map.class
        );

        if (findDocResponse.hits().hits().isEmpty())
            return List.of();

        String docId = findDocResponse.hits().hits().get(0).id();

        SearchResponse<Map> similarResponse = client.search(
                s -> s
                        .index(index)
                        .size(30)
                        .query(q -> q
                                .moreLikeThis(mlt -> mlt
                                        .fields("filename", "content")
                                        .like(like -> like
                                                .document(doc -> doc
                                                        .index(index)
                                                        .id(docId)
                                                )
                                        )
                                        .minTermFreq(1)
                                        .minDocFreq(1)
                                        .minimumShouldMatch("5%")
                                )
                        )
                        .sort(st -> st
                                .score(sc -> sc.order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))
                        ),
                Map.class
        );

        List<Map<String, Object>> results = new ArrayList<>();
        for (Hit<Map> hit : similarResponse.hits().hits()) {
            Map<String, Object> doc = new HashMap<>(hit.source());
            doc.remove("content");
            results.add(doc);
        }

        return results;
    }

}
