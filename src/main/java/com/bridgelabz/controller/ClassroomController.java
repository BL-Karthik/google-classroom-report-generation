
package com.bridgelabz.controller;
import com.bridgelabz.service.GoogleClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/classroom")
public class ClassroomController {

    @Autowired
    private GoogleClassroomService classroomService;

    @Autowired
    private OAuth2AuthorizedClientService clientService;
    Logger logger = Logger.getLogger("ClassroomController");

    private String getAccessToken(OAuth2AuthenticationToken authToken) {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                authToken.getAuthorizedClientRegistrationId(),
                authToken.getName());

        if (client == null || client.getAccessToken() == null) {
            throw new RuntimeException("No valid access token found.");
        }
        return client.getAccessToken().getTokenValue();
    }

    @GetMapping("/all-student-submissions")
    public ResponseEntity<?> exportAllBatches(OAuth2AuthenticationToken authToken) {
        try {
            long startTime = System.currentTimeMillis();
            String token = getAccessToken(authToken);
            List<String> urls = classroomService.exportAllCoursesData(token);
            long endTime = System.currentTimeMillis();
            calculatingTime(startTime, endTime);
            return ResponseEntity.ok("Sheets exported successfully! URLs: " + urls);
        } catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private ResponseEntity<?> exportBatch(OAuth2AuthenticationToken authToken, String batch) {
        try {
            String token = getAccessToken(authToken);
            List<String> urls = classroomService.exportAllCoursesData(token);
            if (urls.isEmpty()) {
                return ResponseEntity.status(404).body("No data found for batch: " + batch);
            }
            return ResponseEntity.ok("Sheets exported successfully for " + batch + "! URLs: " + urls);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    public void calculatingTime(long startTime, long endTime)
    {
        long durationSeconds = (endTime - startTime) / 1000;
        // Format as "X minutes Y seconds"
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        String timeFormatted = (minutes > 0 ? minutes + (minutes == 1 ? " minute " : " minutes ") : "") +
                (seconds > 0 ? seconds + (seconds == 1 ? " second" : " seconds") : "") +
                (minutes == 0 && seconds == 0 ? "0 seconds" : "");
        logger.info("Total time taken by the application: " + timeFormatted);
    }
}
