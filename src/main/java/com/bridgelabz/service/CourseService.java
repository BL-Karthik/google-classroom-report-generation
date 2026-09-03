package com.bridgelabz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class CourseService {

    private static final Logger logger = Logger.getLogger(CourseService.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();

    public List<JsonNode> getAllCourses(String token) {
        WebClient client = buildClient(token);
        List<JsonNode> list = new ArrayList<>();

        try {
            String coursesJson = client.get()
                    .uri("/v1/courses?teacherId=me")
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            response -> {
                                logger.warning("Google Classroom API returned 4xx error while fetching courses");
                                return response.createException();
                            }
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            response -> {
                                logger.severe("Google Classroom API returned 5xx error while fetching courses");
                                return response.createException();
                            }
                    )
                    .bodyToMono(String.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        logger.severe("Error fetching courses: " + ex.getMessage());
                        return java.util.Optional.of("{}").map(reactor.core.publisher.Mono::just).get();
                    })
                    .block();

            JsonNode root = mapper.readTree(coursesJson).path("courses");
            if (root.isArray()) {
                root.forEach(list::add);
            }
        } catch (Exception e) {
            logger.severe("Unexpected error while fetching courses: " + e.getMessage());
        }

        return list; // returns empty if failed
    }

    private WebClient buildClient(String token) {
        return WebClient.builder()
                .baseUrl("https://classroom.googleapis.com")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }
}
