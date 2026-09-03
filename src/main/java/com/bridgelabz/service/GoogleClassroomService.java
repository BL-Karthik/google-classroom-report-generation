

package com.bridgelabz.service;
import com.bridgelabz.csvUtiles.CSVWriterUtil;
import com.fasterxml.jackson.databind.JsonNode;
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.http.HttpRequestInitializer;
//import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.jackson2.JacksonFactory;
//import com.google.api.services.drive.Drive;
//import com.google.api.services.sheets.v4.Sheets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//import java.io.IOException;
//import java.security.GeneralSecurityException;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.*;
import java.util.logging.Logger;





@Service
public class GoogleClassroomService {
    @Autowired
    private CourseService courseService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseworkService courseworkService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private GoogleSheetsDriveService sheetExporterService;

    private static final String DRIVE_FOLDER_ID = "1pI5yE2thXmvLlw39H6jZ-0FschsQffhJ";
    private static final Logger logger = Logger.getLogger("GoogleClassroomService");
    private static final String APPLICATION_NAME = "Google Classroom CSV Exporter";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public List<String> exportAllCoursesData(String accessToken) throws Exception {
        List<String> savedSheetsUrls = new ArrayList<>();
        ConcurrentMap<String, Map<String, Map<String, SimpleEntry<String, String>>>> batchStudentDataMap = new ConcurrentHashMap<>();
        ConcurrentMap<String, Map<String, List<String>>> batchCourseworkMap = new ConcurrentHashMap<>();

        Map<String, Integer> batchTotals = new ConcurrentHashMap<>();
        Map<String, Integer> batchSubmitted = new ConcurrentHashMap<>();
        Map<String, Integer> batchNotSubmitted = new ConcurrentHashMap<>();
        Map<String, Map<String, Integer>> batchSubmissionRanges = new ConcurrentHashMap<>();

        List<JsonNode> courses = sheetExporterService.executeWithRetry(() -> courseService.getAllCourses(accessToken), 4);
        sheetExporterService.throttle();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (JsonNode course : courses) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String courseId = course.path("id").asText();
                    String courseName = course.path("name").asText().replaceAll("[^a-zA-Z0-9]", "_");
                    String batchKey = getBatchKeyFromCourseName(courseName);

                    boolean isTeacher = course.path("courseState").asText().equals("ACTIVE") && course.has("teacherFolder");
                    if (!isTeacher) {
                        logger.info("Skipping course " + courseName + " because you are not a teacher.");
                        return;
                    }

                    Map<String, Map<String, String>> studentsData = sheetExporterService.executeWithRetry(
                            () -> studentService.getStudents(courseId, accessToken), 4);
                    sheetExporterService.throttle();

                    Map<String, String> courseworkMap = sheetExporterService.executeWithRetry(
                            () -> courseworkService.getCourseworkTitles(courseId, accessToken), 4);
                    sheetExporterService.throttle();

                    List<String> courseworkTitles = new ArrayList<>(courseworkMap.values());

                    submissionService.getSubmissions(courseId, accessToken, courseworkMap, studentsData);
                    sheetExporterService.throttle();

                    for (Map<String, String> student : studentsData.values()) {
                        for (String title : courseworkTitles) {
                            student.putIfAbsent(title, "Missing");
                        }
                    }

                    CSVWriterUtil.writeCSV(courseName, studentsData, courseworkTitles);

                    Map<String, SimpleEntry<String, String>> convertedStudentData = new LinkedHashMap<>();
                    for (Map.Entry<String, Map<String, String>> entry : studentsData.entrySet()) {
                        Map<String, String> studentMap = entry.getValue();
                        String studentName = studentMap.getOrDefault("name", entry.getKey());
                        String email = studentMap.getOrDefault("email", "");

                        StringBuilder rowBuilder = new StringBuilder();
                        for (String title : courseworkTitles) {
                            rowBuilder.append(",").append(studentMap.getOrDefault(title, "Missing"));
                        }

                        convertedStudentData.put(studentName, new SimpleEntry<>(email, rowBuilder.substring(1)));
                    }

                    batchStudentDataMap.computeIfAbsent(batchKey, k -> new ConcurrentHashMap<>())
                            .put(courseName, convertedStudentData);

                    batchCourseworkMap.computeIfAbsent(batchKey, k -> new ConcurrentHashMap<>())
                            .put(courseName, courseworkTitles);

                    int total = convertedStudentData.size();
                    int submittedCount = 0;
                    for (SimpleEntry<String, String> entry : convertedStudentData.values()) {
                        String[] statuses = entry.getValue().split(",");
                        long submissions = Arrays.stream(statuses)
                                .filter(s -> s.trim().matches("(?i)submitted|turned in")).count();
                        if (submissions > 0) submittedCount++;
                    }
                    int notSubmittedCount = total - submittedCount;

                    batchTotals.merge(batchKey, total, Integer::sum);
                    batchSubmitted.merge(batchKey, submittedCount, Integer::sum);
                    batchNotSubmitted.merge(batchKey, notSubmittedCount, Integer::sum);

                    Map<String, Integer> ranges = batchSubmissionRanges.computeIfAbsent(batchKey, k -> new ConcurrentHashMap<>());
                    for (SimpleEntry<String, String> entry : convertedStudentData.values()) {
                        String[] statuses = entry.getValue().split(",");
                        long submissions = Arrays.stream(statuses)
                                .filter(s -> s.trim().matches("(?i)submitted|turned in")).count();
                        float percentage = total > 0 ? (submissions * 100.0f / statuses.length) : 0;
                        if (percentage <= 25) ranges.merge("upTo25", 1, Integer::sum);
                        else if (percentage <= 50) ranges.merge("25To50", 1, Integer::sum);
                        else if (percentage <= 75) ranges.merge("50To75", 1, Integer::sum);
                        else ranges.merge("75To100", 1, Integer::sum);
                    }

                } catch (Exception e) {
                    logger.severe("Error processing course: " + e.getMessage());
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        Map<String, Map<String, Map<String, SimpleEntry<String, String>>>> orderedBatchStudentDataMap = new TreeMap<>(batchStudentDataMap);
        for (Map.Entry<String, Map<String, Map<String, SimpleEntry<String, String>>>> entry : orderedBatchStudentDataMap.entrySet()) {
            entry.setValue(new LinkedHashMap<>(entry.getValue()));
        }

        Map<String, Map<String, List<String>>> orderedBatchCourseworkMap = new TreeMap<>(batchCourseworkMap);
        for (Map.Entry<String, Map<String, List<String>>> entry : orderedBatchCourseworkMap.entrySet()) {
            entry.setValue(new LinkedHashMap<>(entry.getValue()));
        }

        for (String batch : orderedBatchStudentDataMap.keySet()) {
            sheetExporterService.resetTotals();
            String sheetUrl = sheetExporterService.exportToGoogleSheetForBatch(
                    batch,
                    orderedBatchStudentDataMap.get(batch),
                    orderedBatchCourseworkMap.get(batch),
                    accessToken,
                    DRIVE_FOLDER_ID
            );
            savedSheetsUrls.add(sheetUrl);
            logger.info("Sheet created for batch " + batch + ": " + sheetUrl);
        }

        sheetExporterService.appendToAllBatchesReportWithStats(
                sheetExporterService.getSheetsService(accessToken),
                sheetExporterService.getDriveService(accessToken),
                accessToken,
                batchTotals,
                batchSubmitted,
                batchNotSubmitted,
                batchSubmissionRanges,
                DRIVE_FOLDER_ID
        );

        return savedSheetsUrls;
    }

    private String getBatchKeyFromCourseName(String courseName) {
        if (courseName.matches(".*B1P\\d+.*")) return "B1";
        if (courseName.matches(".*B2P\\d+.*")) return "B2";
        if (courseName.matches(".*B3P\\d+.*")) return "B3";
        if (courseName.matches(".*B4P\\d+.*")) return "B4";
        return "Other_Batches";
    }
}
