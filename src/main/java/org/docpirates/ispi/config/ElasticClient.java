package org.docpirates.ispi.config;

import lombok.Data;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Data
@Configuration
public class ElasticClient {

    public static RestHighLevelClient client;

    @Bean
    public static RestHighLevelClient createClient() {
        if (client == null) {
            RestClientBuilder builder = RestClient.builder(
                    new HttpHost("localhost", 9200, "http")
            );
            client = new RestHighLevelClient(builder);
        }
        return client;
    }

    public static void closeClient() throws IOException {
        if (client != null)
            client.close();
    }
}

