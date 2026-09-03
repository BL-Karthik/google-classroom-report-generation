package com.bridgelabz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class StudentService {
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Map<String, String>> getStudents(String courseId, String token) throws Exception {
        WebClient client = buildClient(token);
        Map<String, Map<String, String>> studentMap = new LinkedHashMap<>();
        String pageToken = null;

        do {
            String uri = "/v1/courses/" + courseId + "/students";
            if (pageToken != null) uri += "?pageToken=" + pageToken;

            String json = client.get().uri(uri).retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(json);
            pageToken = root.path("nextPageToken").asText(null);

            for (JsonNode student : root.path("students")) {
                String userId = student.path("userId").asText();
                String name = student.path("profile").path("name").path("fullName").asText("Unknown");
                String email = student.path("profile").path("emailAddress").asText("Unknown");

                Map<String, String> data = new LinkedHashMap<>();
                data.put("name", name);
                data.put("email", email);
                studentMap.put(userId, data);
            }
        } while (pageToken != null);

        return studentMap;
    }

    public Map<String, List<String>> getStudentNameEmailMap(String courseId, String token) throws Exception {
        Map<String, Map<String, String>> studentData = getStudents(courseId, token);
        Map<String, List<String>> nameEmailMap = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : studentData.entrySet()) {
            String userId = entry.getKey();
            String name = entry.getValue().getOrDefault("name", "Unknown");
            String email = entry.getValue().getOrDefault("email", "Unknown");
            nameEmailMap.put(userId, List.of(name, email));
        }

        return nameEmailMap;
    }

    private WebClient buildClient(String token) {
        return WebClient.builder()
                .baseUrl("https://classroom.googleapis.com")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }
}
