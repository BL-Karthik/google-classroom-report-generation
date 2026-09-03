package com.bridgelabz.service;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Service
public class GoogleSheetsDriveService {
    private static final Logger logger = Logger.getLogger("GoogleSheetsDriveService");
    private static final String APPLICATION_NAME = "Google Classroom CSV Exporter";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    // Removed @Autowired GoogleClassroomService googleClassroomService; to break the cycle

    private int totalSubmitted = 0;
    private int totalNotSubmitted = 0;

    public void resetTotals() {
        totalSubmitted = 0;
        totalNotSubmitted = 0;
    }

    public int getTotalSubmitted() {
        return totalSubmitted;
    }

    public int getTotalNotSubmitted() {
        return totalNotSubmitted;
    }

    private String sanitizeSheetName(String sheetName) {
        return sheetName.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private void updateSheetData(Sheets sheetsService, String spreadsheetId, String sheetName, String range, List<List<Object>> data) throws IOException {
        ValueRange body = new ValueRange().setValues(data);
        UpdateValuesResponse result = sheetsService.spreadsheets().values()
                .update(spreadsheetId, sheetName + "!" + range, body)
                .setValueInputOption("RAW")
                .execute();
        logger.info("Update result: " + result.getUpdatedCells() + " cells updated in sheet: " + sheetName);
    }

    public String exportToGoogleSheetForBatch(
            String batchName,
            Map<String, Map<String, SimpleEntry<String, String>>> courseStudentData,
            Map<String, List<String>> courseWorkTitles,
            String accessToken,
            String driveFolderId
    ) throws IOException {
        try {
            resetTotals();

            Sheets sheetsService = getSheetsService(accessToken);
            Drive driveService = getDriveService(accessToken);

            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String finalBatchName = batchName + "_" + timestamp;

            String query = String.format(
                    "name contains '%s' and '%s' in parents and mimeType='application/vnd.google-apps.spreadsheet' and trashed=false",
                    batchName, driveFolderId
            );
            FileList result = driveService.files().list().setQ(query).execute();

            if (!result.getFiles().isEmpty()) {
                String existingId = result.getFiles().get(0).getId();
                driveService.files().delete(existingId).execute();
                logger.info("Deleted old spreadsheet for batch: " + batchName + " (ID: " + existingId + ")");
            }

            logger.info("Creating new spreadsheet with title: " + finalBatchName);
            Spreadsheet spreadsheet = new Spreadsheet()
                    .setProperties(new SpreadsheetProperties().setTitle(finalBatchName));
            spreadsheet = sheetsService.spreadsheets().create(spreadsheet).execute();
            String spreadsheetId = spreadsheet.getSpreadsheetId();

            List<Request> sheetRequests = new ArrayList<>();
            Map<String, String> sanitizedNameMap = new HashMap<>();

            for (String courseName : courseStudentData.keySet()) {
                String sheetTitle = sanitizeSheetName(courseName);
                if (sheetTitle.length() > 100) {
                    sheetTitle = sheetTitle.substring(0, 100);
                }
                sanitizedNameMap.put(courseName, sheetTitle);

                AddSheetRequest addSheetRequest = new AddSheetRequest()
                        .setProperties(new SheetProperties().setTitle(sheetTitle));
                sheetRequests.add(new Request().setAddSheet(addSheetRequest));
            }

            // Add Engagement Report sheet
            AddSheetRequest addSummarySheetRequest = new AddSheetRequest()
                    .setProperties(new SheetProperties().setTitle("Engagement Report"));
            sheetRequests.add(new Request().setAddSheet(addSummarySheetRequest));

            if (!sheetRequests.isEmpty()) {
                BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest().setRequests(sheetRequests);
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute();
                logger.info("Added sheets for each course and Engagement Report.");
            }

            List<ValueRange> allUpdates = new ArrayList<>();
            for (Map.Entry<String, Map<String, SimpleEntry<String, String>>> courseEntry : courseStudentData.entrySet()) {
                String courseName = courseEntry.getKey();
                Map<String, SimpleEntry<String, String>> studentRows = courseEntry.getValue();
                List<String> courseworkTitles = courseWorkTitles.getOrDefault(courseName, Collections.emptyList());

                String sheetTitle = sanitizedNameMap.get(courseName);
                List<List<Object>> values = new ArrayList<>();

                List<Object> header = new ArrayList<>();
                header.add("Name");
                header.add("Email");
                header.addAll(courseworkTitles);
                header.add("Submissions");
                header.add("Percentage");
                values.add(header);

                for (Map.Entry<String, SimpleEntry<String, String>> studentEntry : studentRows.entrySet()) {
                    String studentName = studentEntry.getKey();
                    String email = studentEntry.getValue().getKey();
                    String[] statusValues = studentEntry.getValue().getValue().split(",");

                    List<Object> row = new ArrayList<>();
                    row.add(studentName);
                    row.add(email);

                    int submissions = 0;
                    for (String status : statusValues) {
                        String trimmedStatus = status.trim();
                        row.add(trimmedStatus);
                        if ("submitted".equalsIgnoreCase(trimmedStatus) || "turned in".equalsIgnoreCase(trimmedStatus)) {
                            submissions++;
                            totalSubmitted++;
                        } else {
                            totalNotSubmitted++;
                        }
                    }

                    float percentage = statusValues.length > 0 ? ((float) submissions / statusValues.length) * 100 : 0;
                    row.add(submissions);
                    row.add(String.format("%.2f%%", percentage));

                    values.add(row);
                }

                ValueRange courseDataRange = new ValueRange()
                        .setRange(sheetTitle + "!A1")
                        .setValues(values);
                allUpdates.add(courseDataRange);
            }
            System.out.println("Total courses in courseStudentData: " + courseStudentData.size());

            for(String course : courseStudentData.keySet()){
                System.out.println(course);
            }
            // Prepare Engagement Report data
            List<List<Object>> labLevelData = new ArrayList<>();
            labLevelData.add(Arrays.asList(
                    "Lab Id", "Total", "# Submitted", "# Not Submitted",
                    "Upto 25% Submission", "25% - 50% Submission",
                    "50% - 75% Submission", "75% - 100% Submission"
            ));

            int batchTotal = 0;
            int batchSubmitted = 0;
            int batchNotSubmitted = 0;
            int batchUpTo25 = 0, batch25To50 = 0, batch50To75 = 0, batch75To100 = 0;

            for (Map.Entry<String, Map<String, SimpleEntry<String, String>>> courseEntry : courseStudentData.entrySet()) {
                Map<String, SimpleEntry<String, String>> studentRows = courseEntry.getValue();
                int total = studentRows.size();
                int submittedCount = 0;
                int notSubmittedCount = 0;
                int upTo25 = 0, between25And50 = 0, between50And75 = 0, between75And100 = 0;

                for (SimpleEntry<String, String> studentEntry : studentRows.values()) {
                    String[] statuses = studentEntry.getValue().split(",");
                    long submissions = Arrays.stream(statuses)
                            .filter(s -> s.trim().matches("(?i)submitted|turned in")).count();
                    float percentage = statuses.length > 0 ? (submissions * 100.0f / statuses.length) : 0;
                    if (submissions > 0) submittedCount++;
                    else notSubmittedCount++;
                    if (percentage <= 25) upTo25++;
                    else if (percentage <= 50) between25And50++;
                    else if (percentage <= 75) between50And75++;
                    else between75And100++;
                }

                labLevelData.add(Arrays.asList(
                        courseEntry.getKey(),
                        total,
                        submittedCount,
                        notSubmittedCount,
                        upTo25,
                        between25And50,
                        between50And75,
                        between75And100
                ));

                batchTotal += total;
                batchSubmitted += submittedCount;
                batchNotSubmitted += notSubmittedCount;
                batchUpTo25 += upTo25;
                batch25To50 += between25And50;
                batch50To75 += between50And75;
                batch75To100 += between75And100;
            }


            //  to skip header row
            List<List<Object>> dataRows = labLevelData.subList(1, labLevelData.size());

            dataRows.sort((row1, row2) -> {
                String id1 = row1.get(0).toString().toUpperCase();
                String id2 = row2.get(0).toString().toUpperCase();

                int pIndex1 = id1.indexOf("P");
                int pIndex2 = id2.indexOf("P");

                if (pIndex1 == -1) pIndex1 = 0;
                if (pIndex2 == -1) pIndex2 = 0;

                int underscoreIndex1 = id1.indexOf("_", pIndex1);
                int underscoreIndex2 = id2.indexOf("_", pIndex2);

                if (underscoreIndex1 == -1) underscoreIndex1 = id1.length();
                if (underscoreIndex2 == -1) underscoreIndex2 = id2.length();

                String num1 = id1.substring(pIndex1 + 1, underscoreIndex1);
                String num2 = id2.substring(pIndex2 + 1, underscoreIndex2);

                // Compare numerically
                try {
                    return Integer.compare(Integer.parseInt(num1), Integer.parseInt(num2));
                } catch (NumberFormatException e) {
                    return num1.compareTo(num2);
                }
            });




            // Batch summary starting at A23
            List<List<Object>> batchSummary = new ArrayList<>();
            batchSummary.add(Arrays.asList(
                    "Batch #", "Total", "# Submitted", "# Not Submitted",
                    "Upto 25% Submission", "25% - 50% Submission",
                    "50% - 75% Submission", "75% - 100% Submission"
            ));

            batchSummary.add(Arrays.asList(
                    "Batch " + batchName.replace("B", ""),
                    batchTotal,
                    batchSubmitted,
                    batchNotSubmitted,
                    String.format("%.2f%%", (batchUpTo25 * 100.0 / (batchTotal == 0 ? 1 : batchTotal))),
                    String.format("%.2f%%", (batch25To50 * 100.0 / (batchTotal == 0 ? 1 : batchTotal))),
                    String.format("%.2f%%", (batch50To75 * 100.0 / (batchTotal == 0 ? 1 : batchTotal))),
                    String.format("%.2f%%", (batch75To100 * 100.0 / (batchTotal == 0 ? 1 : batchTotal)))
            ));

            ValueRange labLevelDataRange = new ValueRange()
                    .setRange("Engagement Report!A1")
                    .setValues(labLevelData);
            allUpdates.add(labLevelDataRange);


            ValueRange batchSummaryRange = new ValueRange()
                    .setRange("Engagement Report!A"+(labLevelData.size()+10))
                    .setValues(batchSummary);
            allUpdates.add(batchSummaryRange);

            BatchUpdateValuesRequest batchRequestBody = new BatchUpdateValuesRequest()
                    .setValueInputOption("RAW")
                    .setData(allUpdates);

            BatchUpdateValuesResponse batchResponse = sheetsService.spreadsheets().values()
                    .batchUpdate(spreadsheetId, batchRequestBody)
                    .execute();

            logger.info("Batch update response: " + batchResponse.getTotalUpdatedCells() + " cells updated in total.");

            driveService.files().update(spreadsheetId, null)
                    .setAddParents(driveFolderId)
                    .setFields("id, parents")
                    .execute();
            logger.info("Moved spreadsheet to Drive folder: " + driveFolderId);

            return "https://docs.google.com/spreadsheets/d/" + spreadsheetId;

        } catch (GeneralSecurityException e) {
            logger.severe("Failed to export sheet due to security exception: " + e.getMessage());
            throw new RuntimeException("Failed to export sheet: " + e.getMessage(), e);
        }
    }


    public String appendToAllBatchesReportWithStats(
            Sheets sheetsService,
            Drive driveService,
            String accessToken,
            Map<String, Integer> batchTotals,
            Map<String, Integer> batchSubmitted,
            Map<String, Integer> batchNotSubmitted,
            Map<String, Map<String, Integer>> batchSubmissionRanges,
            String folderId
    ) throws IOException {
        String reportName = "All Batches report";
        String query = String.format(
                "name = '%s' and '%s' in parents and mimeType='application/vnd.google-apps.spreadsheet' and trashed=false",
                reportName, folderId
        );
        FileList result = driveService.files().list().setQ(query).execute();

        String masterId;
        if (result.getFiles().isEmpty()) {
            File fileMetadata = new File()
                    .setName(reportName)
                    .setMimeType("application/vnd.google-apps.spreadsheet")
                    .setParents(Collections.singletonList(folderId));
            File createdFile = driveService.files()
                    .create(fileMetadata)
                    .setFields("id")
                    .execute();
            masterId = createdFile.getId();

            List<List<Object>> header = Collections.singletonList(Arrays.asList(
                    "Batch #", "Total", "# Submitted", "# Not Submitted",
                    "Up to 25% Submission", "25% - 50% Submission",
                    "50% - 75% Submission", "75% - 100% Submission", "% Submission"
            ));
            sheetsService.spreadsheets().values()
                    .append(masterId, "Sheet1!A1", new ValueRange().setValues(header))
                    .setValueInputOption("RAW")
                    .execute();
        } else {
            masterId = result.getFiles().get(0).getId();
            // Clear existing data in the range A2:I (excluding header)
            ClearValuesRequest clearRequest = new ClearValuesRequest();
            sheetsService.spreadsheets().values()
                    .clear(masterId, "Sheet1!A2:I", clearRequest)
                    .execute();
        }

        List<List<Object>> data = new ArrayList<>();
        for (String batch : batchTotals.keySet()) {
            int total = batchTotals.get(batch);
            int submitted = batchSubmitted.get(batch);
            int notSubmitted = batchNotSubmitted.get(batch);
            Map<String, Integer> ranges = batchSubmissionRanges.getOrDefault(batch, new HashMap<>());
            int upTo25 = ranges.getOrDefault("upTo25", 0);
            int _25To50 = ranges.getOrDefault("25To50", 0);
            int _50To75 = ranges.getOrDefault("50To75", 0);
            int _75To100 = ranges.getOrDefault("75To100", 0);

            double upTo25Percent = total > 0 ? (upTo25 * 100.0 / total) : 0;
            double _25To50Percent = total > 0 ? (_25To50 * 100.0 / total) : 0;
            double _50To75Percent = total > 0 ? (_50To75 * 100.0 / total) : 0;
            double _75To100Percent = total > 0 ? (_75To100 * 100.0 / total) : 0;
            double submissionPercent = total > 0 ? (submitted * 100.0 / total) : 0;

            data.add(Arrays.asList(
                    "Batch " + batch.replace("B", ""),
                    total,
                    submitted,
                    notSubmitted,
                    String.format("%.2f%%", upTo25Percent),
                    String.format("%.2f%%", _25To50Percent),
                    String.format("%.2f%%", _50To75Percent),
                    String.format("%.2f%%", _75To100Percent),
                    String.format("%.2f%%", submissionPercent)
            ));
        }

        // Add total row
        int totalAll = batchTotals.values().stream().mapToInt(Integer::intValue).sum();
        int submittedAll = batchSubmitted.values().stream().mapToInt(Integer::intValue).sum();
        int notSubmittedAll = batchNotSubmitted.values().stream().mapToInt(Integer::intValue).sum();
        int upTo25All = batchSubmissionRanges.values().stream().mapToInt(m -> m.getOrDefault("upTo25", 0)).sum();
        int _25To50All = batchSubmissionRanges.values().stream().mapToInt(m -> m.getOrDefault("25To50", 0)).sum();
        int _50To75All = batchSubmissionRanges.values().stream().mapToInt(m -> m.getOrDefault("50To75", 0)).sum();
        int _75To100All = batchSubmissionRanges.values().stream().mapToInt(m -> m.getOrDefault("75To100", 0)).sum();

        double upTo25PercentAll = totalAll > 0 ? (upTo25All * 100.0 / totalAll) : 0;
        double _25To50PercentAll = totalAll > 0 ? (_25To50All * 100.0 / totalAll) : 0;
        double _50To75PercentAll = totalAll > 0 ? (_50To75All * 100.0 / totalAll) : 0;
        double _75To100PercentAll = totalAll > 0 ? (_75To100All * 100.0 / totalAll) : 0;
        double submissionPercentAll = totalAll > 0 ? (submittedAll * 100.0 / totalAll) : 0;

        data.add(Arrays.asList(
                "Total",
                totalAll,
                submittedAll,
                notSubmittedAll,
                String.format("%.2f%%", upTo25PercentAll),
                String.format("%.2f%%", _25To50PercentAll),
                String.format("%.2f%%", _50To75PercentAll),
                String.format("%.2f%%", _75To100PercentAll),
                String.format("%.2f%%", submissionPercentAll)
        ));



          data.sort((row1,row2)->{
              String id1=row1.get(0).toString();
              String id2=row2.get(0).toString();
              return  id1.substring(1,id1.length()).compareTo(id2.substring(1,id2.length()));
          })
        ;
        sheetsService.spreadsheets().values()
                .append(masterId, "Sheet1!A2", new ValueRange().setValues(data))
                .setValueInputOption("RAW")
                .setInsertDataOption("INSERT_ROWS")
                .execute();

        return masterId;
    }

    public void throttle() {
        try {
            Thread.sleep(1000); // 1-second delay between requests
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public HttpRequestInitializer getRequestInitializer(String accessToken) {
        return request -> {
            request.getHeaders().setAuthorization("Bearer " + accessToken);
            // Removed interceptor since it’s not the right place to check response status
        };
    }

    public <T> T executeWithRetry(Callable<T> callable, int maxRetries) throws Exception {
        int retries = 0;
        long maxWaitTime = 8000; // Cap wait time at 8 seconds
        while (retries < maxRetries) {
            try {
                T result = callable.call();
                if (result == null) {
                    logger.severe(String.format("Null result from callable (attempt %d/%d). Treating as failure.", retries + 1, maxRetries));
                    if (retries == maxRetries - 1) {
                        throw new IOException("Callable returned null result after max retries");
                    }
                } else {
                    return result;
                }
            } catch (HttpResponseException e) {
                if (e.getStatusCode() == 429 && retries < maxRetries - 1) {
                    String retryAfter = e.getHeaders().getFirstHeaderStringValue("Retry-After");
                    long waitTime;
                    if (retryAfter != null && retryAfter.matches("\\d+")) {
                        waitTime = Math.min(Long.parseLong(retryAfter) * 1000, maxWaitTime);
                    } else {
                        waitTime = Math.min((long) Math.pow(2, retries) * 1000, maxWaitTime);
                    }
                    logger.warning(String.format("Rate limit exceeded (attempt %d/%d). Status: %d, Retry-After: %s, Waiting: %dms, Error: %s",
                            retries + 1, maxRetries, e.getStatusCode(), retryAfter, waitTime, e.getMessage()));
                    Thread.sleep(waitTime);
                    retries++;
                } else {
                    logger.severe(String.format("Unrecoverable HTTP error (attempt %d/%d). Status: %d, Error: %s",
                            retries + 1, maxRetries, e.getStatusCode(), e.getMessage()));
                    throw e;
                }
            } catch (Exception e) { // Catch all exceptions to debug silent failures
                if (retries < maxRetries - 1) {
                    long waitTime = Math.min((long) Math.pow(2, retries) * 1000, maxWaitTime);
                    logger.warning(String.format("Unexpected error (attempt %d/%d). Waiting: %dms, Error: %s, StackTrace: %s",
                            retries + 1, maxRetries, waitTime, e.getMessage(), Arrays.toString(e.getStackTrace())));
                    Thread.sleep(waitTime);
                    retries++;
                } else {
                    logger.severe(String.format("Exhausted retries (attempt %d/%d). Final error: %s, StackTrace: %s",
                            retries + 1, maxRetries, e.getMessage(), Arrays.toString(e.getStackTrace())));
                    throw e;
                }
            }
        }
        // Final attempt
        try {
            T result = callable.call();
            if (result == null) {
                throw new IOException("Callable returned null result on final attempt");
            }
            return result;
        } catch (Exception e) {
            logger.severe(String.format("Final attempt failed. Error: %s, StackTrace: %s",
                    e.getMessage(), Arrays.toString(e.getStackTrace())));
            throw e;
        }
    }



    public Sheets getSheetsService(String accessToken) throws GeneralSecurityException, IOException {
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, getRequestInitializer(accessToken))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public Drive getDriveService(String accessToken) throws GeneralSecurityException, IOException {
        return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, getRequestInitializer(accessToken))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}