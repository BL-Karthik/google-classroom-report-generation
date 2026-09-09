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

    public List<JsonNode> getAllCourses(String token, String word) {

        WebClient client = buildClient(token);
        List<JsonNode> list = new ArrayList<>();
        try {
            logger.info("Fetching courses from Google Classroom...");
            String coursesJson = client.get()
                    .uri("/v1/courses?teacherId=me")
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            response -> {logger.warning("Google Classroom API returned 4xx error");
                                return response.createException();}
                    )

                    .onStatus(
                            status -> status.is5xxServerError(),
                            response -> {
                                logger.severe(
                                        "Google Classroom API returned 5xx error");
                                return response.createException();
                            }
                    )

                    .bodyToMono(String.class)

                    .onErrorResume(
                            WebClientResponseException.class,
                            ex -> {logger.severe(
                                        "Error fetching courses: " + ex.getStatusCode() + " - " + ex.getMessage()
                                );
                                return reactor.core.publisher.Mono.just("{}");
                            }
                    ).block();

            JsonNode root =mapper.readTree(coursesJson).path("courses");

            if (!root.isArray()) {
                logger.warning("No courses array returned by Google Classroom"
                );
                return list;
            }

            logger.info("Total courses received from Google Classroom: " + root.size()
            );

            for (JsonNode course : root) {
                String courseId = course.path("id").asText();

                String courseName = course.path("name").asText();

                String courseState = course.path("courseState").asText();

                logger.info(
                        "Course found: " + courseName + " | ID: " + courseId + " | State: " + courseState
                );


                 /* GoogleClassroomService will handle the
                 * word-based filtering.
                 */

                list.add(course);
            }

            logger.info(
                    "Courses returned to GoogleClassroomService: " + list.size());

        } catch (Exception e) {
            logger.severe(
                    "Unexpected error while fetching courses: "
                            + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    private WebClient buildClient(String token) {

        return WebClient.builder()
                .baseUrl("https://classroom.googleapis.com")
                .defaultHeader(
                        "Authorization",
                        "Bearer " + token
                ).build();
    }
}