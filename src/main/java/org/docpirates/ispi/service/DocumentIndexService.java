package org.docpirates.ispi.service;

import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.dto.DocumentForIndexing;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.docpirates.ispi.config.ElasticClient.client;

@Service
@RequiredArgsConstructor
public class DocumentIndexService {

    public static final String DOCUMENT_BASE_PATH = "path";

    public List<DocumentForIndexing> readDocumentsFromDirectory(Path rootDir) throws IOException {
        List<DocumentForIndexing> documents = new ArrayList<>();

        Files.walk(rootDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        String content = Files.readString(file, StandardCharsets.UTF_8);
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

    public void indexDocuments(RestHighLevelClient client, List<DocumentForIndexing> documents) {
        for (DocumentForIndexing doc : documents) {
            try {
                addDocument(doc);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addDocument(DocumentForIndexing document) throws IOException {
        try {
            IndexRequest request = new IndexRequest("doc_index")
                    .source("{\"path\": \"" + document.getPath() + "\", " +
                            "\"filename\": \"" + document.getFilename() + "\", " +
                            "\"content\": \"" + escapeJson(document.getContent()) + "\"}",
                            XContentType.JSON);

            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            System.out.println("Indexed: " + response.getId());
        } catch (IOException e) {
            System.err.println("Failed to index: " + document.getFilename() + " - " + e.getMessage());
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    public int getTotalDocumentCount() throws IOException {
        CountRequest countRequest = new CountRequest("doc_index");
        CountResponse countResponse = client.count(countRequest, RequestOptions.DEFAULT);
        return (int) countResponse.getCount();
    }

    public List<DocumentForIndexing> searchRelevantDocuments(String userQuery, int from, int to) throws IOException {
        int maxDocs = getTotalDocumentCount();
        if (from < 0) from = 0;
        if (to > maxDocs) to = maxDocs;
        if (from >= to) return List.of();
        int size = to - from;

        SearchRequest searchRequest = new SearchRequest("doc_index");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.multiMatchQuery(userQuery)
                        .field("filename", 3.0f)
                        .field("content", 1.0f))
                .from(from)
                .size(size)
                .sort(SortBuilders.scoreSort().order(SortOrder.DESC));

        searchRequest.source(sourceBuilder);
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        return Arrays.stream(response.getHits().getHits())
                .map(hit -> {
                    Map<String, Object> source = hit.getSourceAsMap();
                    String filename = (String) source.get("filename");
                    String content = (String) source.get("content");
                    String path = (String) source.get("path");
                    return new DocumentForIndexing(filename, content, path);
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> findSimilarToDocumentByTitle(String title) throws IOException {
        String index = "doc_index";

        SearchRequest findDocRequest = new SearchRequest(index);
        SearchSourceBuilder findSourceBuilder = new SearchSourceBuilder();
        findSourceBuilder.query(QueryBuilders.matchQuery("title", title));
        findSourceBuilder.size(1);
        findDocRequest.source(findSourceBuilder);

        SearchResponse findDocResponse = client.search(findDocRequest, RequestOptions.DEFAULT);
        SearchHits hits = findDocResponse.getHits();

        if (hits.getTotalHits().value == 0)
            return Collections.emptyList();

        String docId = hits.getAt(0).getId();

        SearchRequest similarRequest = new SearchRequest(index);
        SearchSourceBuilder similarSourceBuilder = new SearchSourceBuilder();
        MoreLikeThisQueryBuilder.Item[] likeItem = {
                new MoreLikeThisQueryBuilder.Item(index, docId)
        };

        MoreLikeThisQueryBuilder mltQuery = QueryBuilders.moreLikeThisQuery(
                new String[]{"title", "content"},
                null,
                likeItem
        ).minTermFreq(1).minDocFreq(1);

        similarSourceBuilder.query(mltQuery);
        similarSourceBuilder.size(30);
        similarSourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
        similarRequest.source(similarSourceBuilder);

        SearchResponse similarResponse = client.search(similarRequest, RequestOptions.DEFAULT);
        SearchHits similarHits = similarResponse.getHits();

        List<Map<String, Object>> results = new ArrayList<>();
        for (SearchHit hit : similarHits)
            results.add(hit.getSourceAsMap());

        return results;
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
}
