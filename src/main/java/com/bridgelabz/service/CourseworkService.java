package com.bridgelabz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class CourseworkService {

    private final ObjectMapper mapper = new ObjectMapper();

//    public Map<String, String> getCourseworkTitles(String courseId, String token) throws Exception {
//        WebClient client = buildClient(token);
//        Map<String, String> map = new LinkedHashMap<>();
//        String pageToken = null;
//
//        do {
//            String uri = "/v1/courses/" + courseId + "/courseWork";
//            if (pageToken != null) uri += "?pageToken=" + pageToken;
//
//            String json = client.get().uri(uri).retrieve().bodyToMono(String.class).block();
//            JsonNode root = mapper.readTree(json);
//            pageToken = root.path("nextPageToken").asText(null);
//
//            for (JsonNode cw : root.path("courseWork")) {
//                String id = cw.path("id").asText();
//                String title = cw.path("title").asText().replaceAll("[^a-zA-Z0-9_\\- ]", "_");
//                map.put(id, title + " (ID: " + id + ")");
//                System.out.println(" Coursework Titles Collected: " + title);
//            }
//        } while (pageToken != null);
//
//        return map;
//    }

    public Map<String, String> getCourseworkTitles(String courseId, String token) throws Exception {

        WebClient client = buildClient(token);
        Map<String, String> map = new LinkedHashMap<>();
        List<String> titles = new ArrayList<>();

        String pageToken = null;

        do {
            String uri = "/v1/courses/" + courseId + "/courseWork";
            if (pageToken != null) uri += "?pageToken=" + pageToken;

            String json = client.get().uri(uri).retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(json);
            pageToken = root.path("nextPageToken").asText(null);

            for (JsonNode cw : root.path("courseWork")) {

                String id = cw.path("id").asText();
                String title = cw.path("title").asText().replaceAll("[^a-zA-Z0-9_\\- ]", "_");

                map.put(id, title);
                titles.add(title);
            }

        } while (pageToken != null);


        Collections.sort(titles);
        for(String t : titles){
            System.out.println("Coursework Titles Collected: " + t);
        }

        return map;
    }


    private WebClient buildClient(String token) {
        return WebClient.builder()
                .baseUrl("https://classroom.googleapis.com")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }
}
