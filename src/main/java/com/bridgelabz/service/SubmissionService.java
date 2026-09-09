package com.bridgelabz.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
public class SubmissionService {

    private final ObjectMapper mapper = new ObjectMapper();

    private final int size= 20*1024*1024;

    public void getSubmissions(String courseId, String token,
                               Map<String, String> cwMap,
                               Map<String, Map<String, String>> students) throws Exception {
        WebClient client = buildClient(token);

        for (String cwId : cwMap.keySet()) {
            String title = cwMap.get(cwId);
            String pageToken = null;

            do {
                String uri = "/v1/courses/" + courseId + "/courseWork/" + cwId + "/studentSubmissions";
                if (pageToken != null) uri += "?pageToken=" + pageToken;

                String json = client.get().uri(uri).retrieve().bodyToMono(String.class).block();
                JsonNode root = mapper.readTree(json);
                pageToken = root.path("nextPageToken").asText(null);

                for (JsonNode sub : root.path("studentSubmissions")) {
                    String userId = sub.path("userId").asText();
                    String state = sub.path("state").asText();

                    String status = switch (state) {
                        case "TURNED_IN", "RETURNED" -> "Submitted";
                        case "CREATED", "NEW" -> "Assigned";
                        default -> "Missing";
                    };

                    if (students.containsKey(userId)) {
                        students.get(userId).put(title, status);
                    }
                }
            } while (pageToken != null);
        }
    }

    private WebClient buildClient(String token) {
        return WebClient.builder()
                .baseUrl("https://classroom.googleapis.com")
                .defaultHeader("Authorization", "Bearer " + token)
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();
    }
}

