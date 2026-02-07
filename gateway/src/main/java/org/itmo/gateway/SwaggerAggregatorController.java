package org.itmo.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@RestController
@Getter
@Setter
@ConfigurationProperties(prefix = "springdoc.swagger-ui")
public class SwaggerAggregatorController {
    private List<SwaggerUrl> urls;
    @Value("${server.port}")
    private String port;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public SwaggerAggregatorController(WebClient.Builder webClientBuilder,
                                       ObjectMapper objectMapper
    ) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SwaggerUrl {
        private String name;
        private String url;
    }

    @GetMapping("/v3/api-docs/aggregated")
    public Mono<ObjectNode> getAggregatedSwagger() {
        return fetchAllSwaggerSpecs()
                .collectList()
                .map(this::mergeAllSpecs);
    }

    private Flux<ObjectNode> fetchAllSwaggerSpecs() {
        return Flux.fromIterable(urls)
                .filter(swaggerUrl -> !Objects.equals(swaggerUrl.url, "/v3/api-docs/aggregated"))
                .flatMap(swaggerUrl -> fetchSwaggerSpec(
                                "http://localhost:" + port + swaggerUrl.getUrl()
                        )
                                .map(spec -> {
                                    if (spec.get("tags") == null) {
                                        spec.set("tags", objectMapper.createArrayNode());
                                    }
                                    spec.putArray("tags").add(
                                            objectMapper.createObjectNode()
                                                    .put("name", swaggerUrl.getName())
                                                    .put("description", "API для " + swaggerUrl.getName())
                                    );
                                    return spec;
                                })
                                .onErrorResume(e -> Mono.empty())
                );
    }

    private Mono<ObjectNode> fetchSwaggerSpec(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseOpenAPI)
                .filter(Objects::nonNull);
    }

    private ObjectNode parseOpenAPI(String json) {
        try {
            return (ObjectNode) objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private ObjectNode mergeAllSpecs(List<ObjectNode> specs) {
        ObjectNode merged = objectMapper.createObjectNode();
        merged.put("openapi", "3.0.1");

        ObjectNode info = objectMapper.createObjectNode();
        info.put("title", "Application API");
        info.put("version", "1.0.3");
        info.put("description", "Full application API");
        merged.set("info", info);

        ObjectNode allPaths = objectMapper.createObjectNode();
        specs.forEach(spec -> {
            if (spec.get("paths") != null) {
                spec.get("paths").propertyStream().forEach(
                        it -> allPaths.set(it.getKey(), it.getValue())
                );
            }
        });
        merged.set("paths", allPaths);

        ObjectNode components = objectMapper.createObjectNode();
        components.set("schemas", objectMapper.createObjectNode());
        specs.forEach(spec -> {
            if (spec.get("components") != null && spec.get("components").get("schemas") != null) {
                spec.get("components").get("schemas").propertyStream().forEach(
                        it -> ((ObjectNode) components.get("schemas"))
                                .set(it.getKey(), it.getValue())
                );
            }
        });
        merged.set("components", components);

        ObjectNode securitySchemas = objectMapper.createObjectNode();
        specs.forEach(spec -> {
            if (spec.get("components") != null && spec.get("components").get("securitySchemes") != null) {
                spec.get("components").get("securitySchemes").propertyStream().forEach(
                        it -> securitySchemas.set(it.getKey(), it.getValue())
                );
            }
        });
        ((ObjectNode) merged.get("components")).set("securitySchemes", securitySchemas);

        ArrayNode security = objectMapper.createArrayNode();
        specs.forEach(spec -> {
            if (spec.get("security") != null && security.isEmpty()) {
                spec.get("security").values().forEachRemaining(security::add);
            }
        });
        merged.set("security", security);

        return merged;
    }
}
