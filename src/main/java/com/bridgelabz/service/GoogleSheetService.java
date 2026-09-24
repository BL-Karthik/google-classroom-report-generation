package com.bridgelabz.service;

import com.bridgelabz.config.ReportConfig;
import com.bridgelabz.models.AssignmentColumn;
import com.bridgelabz.models.CourseInfo;
import com.bridgelabz.models.CourseSummary;
import com.bridgelabz.models.StudentData;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleSheetService {

    private static final String SPREADSHEET_ID_B1 =
            "1fZsJPXt_6V5e7KnHO0MLE_ihbwrXtI3Ae0Md060bD2Q";

    private static final String SPREADSHEET_ID_B2 =
            "1jgmFkPIFFp71c2EPWMwCXOJ6bG_irJYxFn1rSAoTf_I";

    private static final String SERVICE_ACCOUNT_FILE =
            "service_account.json";


    // ============================================================
    // GOOGLE SHEETS SERVICE
    // ============================================================

    public static Sheets createSheetsService()
            throws Exception {

        try (FileInputStream input =
                     new FileInputStream(
                             SERVICE_ACCOUNT_FILE)) {

            GoogleCredential credential =
                    GoogleCredential
                            .fromStream(input)
                            .createScoped(
                                    Collections.singleton(
                                            SheetsScopes.SPREADSHEETS
                                    )
                            );

            System.out.println(
                    "Authenticated Service Account: "
                            + credential.getServiceAccountId()
            );

            return new Sheets.Builder(
                    credential.getTransport(),
                    credential.getJsonFactory(),
                    credential
            )
                    .setApplicationName(
                            "Classroom Grade Exporter"
                    )
                    .build();
        }
    }


    // ============================================================
    // TEST GOOGLE SHEETS ACCESS
    // ============================================================

    public static boolean testGoogleSheetsAccess() {

        System.out.println(
                "\n======================================"
        );

        System.out.println(
                "GOOGLE SHEETS ACCESS TEST"
        );

        System.out.println(
                "======================================"
        );

        try {

            Sheets sheets =
                    createSheetsService();

            String[] targets = {
                    "B1: " + SPREADSHEET_ID_B1,
                    "B2: " + SPREADSHEET_ID_B2
            };

            boolean allOk = true;

            for (String target : targets) {

                String id =
                        target.substring(
                                target.indexOf(':') + 2
                        );

                System.out.println(
                        "\nChecking spreadsheet: "
                                + target
                );

                Spreadsheet spreadsheet =
                        sheets.spreadsheets()
                                .get(id)
                                .execute();

                System.out.println(
                        "OK - Title: "
                                + spreadsheet
                                .getProperties()
                                .getTitle()
                );
            }

            return allOk;

        } catch (
                com.google.api.client.googleapis.json.GoogleJsonResponseException e) {

            System.out.println(
                    "\nGOOGLE SHEETS ERROR"
            );

            System.out.println(
                    "HTTP Status: "
                            + e.getStatusCode()
            );

            return false;

        } catch (Exception e) {

            System.out.println(
                    "\nUnable to connect to Google Sheets."
            );

            e.printStackTrace();

            return false;
        }
    }


    // ============================================================
    // WEEK FILTER
    // ============================================================

    public static boolean isValidWeekAssignment(
            String title) {

        if (title == null ||
                title.trim().isEmpty()) {

            return false;
        }

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile(
                                "(?i).*\\bweek\\s*(\\d+)\\b.*"
                        )
                        .matcher(title);

        if (!matcher.matches()) {
            return false;
        }

        int week =
                Integer.parseInt(
                        matcher.group(1)
                );

        return week >= 1 &&
                week <= ReportConfig.getMaxWeek();
    }


    // ============================================================
    // GET WEEK ASSIGNMENTS
    // ============================================================

    public static List<AssignmentColumn>
    getWeek1To6Assignments(
            List<AssignmentColumn> assignments) {

        List<AssignmentColumn> validAssignments =
                new ArrayList<>();

        for (AssignmentColumn assignment :
                assignments) {

            if (isValidWeekAssignment(
                    assignment.title)) {

                validAssignments.add(
                        assignment
                );
            }
        }

        return validAssignments;
    }


    // ============================================================
    // EXPORT INDIVIDUAL COURSE SHEET
    // ============================================================

    public static boolean exportToGoogleSheets(
            String spreadsheetId,
            String tabName,
            List<AssignmentColumn> assignments,
            List<StudentData> students) {

        try {

            Sheets sheets =
                    createSheetsService();

            String sheetName =
                    tabName;

            int sheetId =
                    getOrCreateSheet(
                            sheets,
                            spreadsheetId,
                            sheetName
                    );

            // ----------------------------------------------------
            // ONLY SELECTED WEEKS
            // ----------------------------------------------------

            List<AssignmentColumn> validAssignments =
                    getWeek1To6Assignments(
                            assignments
                    );

            int statusCount =
                    validAssignments.size();

            System.out.println(
                    "\nAssignments selected for "
                            + sheetName
                            + ": "
                            + statusCount
            );

            for (AssignmentColumn assignment :
                    validAssignments) {

                System.out.println(
                        "  - "
                                + assignment.title
                );
            }

            // ----------------------------------------------------
            // CREATE HEADER
            // ----------------------------------------------------

            List<List<Object>> values =
                    new ArrayList<>();

            List<Object> header =
                    new ArrayList<>();

            header.add("# Roll Number");
            header.add("Student Name");
            header.add("Submissions");
            header.add("Percentage");

            for (AssignmentColumn assignment :
                    validAssignments) {

                header.add(
                        assignment.title
                                + " - Status"
                );
            }

            values.add(header);

            // ----------------------------------------------------
            // COLUMN LETTERS
            // ----------------------------------------------------

            String firstStatusCol =
                    columnLetter(4);

            String lastStatusCol =
                    columnLetter(
                            4 + statusCount - 1
                    );

            String submissionsCol =
                    columnLetter(2);

            // ----------------------------------------------------
            // STUDENTS
            // ----------------------------------------------------

            for (StudentData student :
                    students) {

                List<Object> row =
                        new ArrayList<>();

                row.add(student.rollNumber);
                row.add(student.name);

                int rowNum =
                        values.size() + 1;

                if (statusCount > 0) {

                    row.add(
                            "=COUNTIF("
                                    + firstStatusCol
                                    + rowNum
                                    + ":"
                                    + lastStatusCol
                                    + rowNum
                                    + ",\"Submitted\")"
                    );

                    row.add(
                            "="
                                    + submissionsCol
                                    + rowNum
                                    + "/"
                                    + statusCount
                    );

                } else {

                    row.add(0);
                    row.add("");
                }

                // ------------------------------------------------
                // STATUS VALUES
                // ------------------------------------------------

                for (AssignmentColumn assignment :
                        validAssignments) {

                    String status =
                            getStudentAssignmentStatus(
                                    student,
                                    assignment,
                                    assignments
                            );

                    row.add(status);
                }

                values.add(row);
            }

            // ----------------------------------------------------
            // ENSURE SHEET SIZE
            // ----------------------------------------------------

            GoogleSheetFormatService.ensureSheetSize(
                    sheets,
                    spreadsheetId,
                    sheetId,
                    values.size(),
                    header.size()
            );

            // ----------------------------------------------------
            // CLEAR OLD DATA
            // ----------------------------------------------------

            clearSheet(
                    sheets,
                    spreadsheetId,
                    sheetName
            );

            // ----------------------------------------------------
            // WRITE DATA
            // ----------------------------------------------------

            ValueRange body =
                    new ValueRange()
                            .setValues(values);

            sheets.spreadsheets()
                    .values()
                    .update(
                            spreadsheetId,
                            sheetName + "!A1",
                            body
                    )
                    .setValueInputOption(
                            "USER_ENTERED"
                    )
                    .execute();

            // ----------------------------------------------------
            // FORMAT
            // ----------------------------------------------------

            GoogleSheetFormatService.formatSheet(
                    sheets,
                    spreadsheetId,
                    sheetId,
                    values.size(),
                    header.size()
            );

            System.out.println(
                    "\nData exported to Google Sheets."
            );

            return true;

        } catch (
                com.google.api.client.googleapis.json.GoogleJsonResponseException e) {

            System.out.println(
                    "\nGoogle Sheets export failed."
            );

            System.out.println(
                    "HTTP Status: "
                            + e.getStatusCode()
            );

            return false;

        } catch (Exception e) {

            System.out.println(
                    "\nGoogle Sheets export failed."
            );

            e.printStackTrace();

            return false;
        }
    }


    // ============================================================
    // STUDENT ASSIGNMENT STATUS
    // ============================================================

    private static String getStudentAssignmentStatus(
            StudentData student,
            AssignmentColumn assignment,
            List<AssignmentColumn> assignments) {

        if (student.assignments == null ||
                student.assignments.isEmpty()) {

            return "Not submitted";
        }

        int originalIndex =
                assignments.indexOf(
                        assignment
                );

        if (originalIndex >= 0 &&
                originalIndex <
                        student.assignments.size()) {

            String status =
                    student.assignments
                            .get(originalIndex)
                            .status;

            return status == null
                    ? "Not submitted"
                    : status;
        }

        return "Not submitted";
    }


    // ============================================================
    // COLUMN LETTER
    // ============================================================

    public static String columnLetter(
            int zeroBasedIndex) {

        StringBuilder letter =
                new StringBuilder();

        int index =
                zeroBasedIndex;

        while (index >= 0) {

            letter.insert(
                    0,
                    (char) (
                            'A'
                                    + index % 26
                    )
            );

            index =
                    index / 26 - 1;
        }

        return letter.toString();
    }


    // ============================================================
    // GET OR CREATE SHEET
    // ============================================================

    public static int getOrCreateSheet(
            Sheets sheets,
            String spreadsheetId,
            String sheetName)
            throws Exception {

        Spreadsheet spreadsheet =
                sheets.spreadsheets()
                        .get(spreadsheetId)
                        .execute();

        if (spreadsheet.getSheets() != null) {

            for (Sheet sheet :
                    spreadsheet.getSheets()) {

                if (sheet.getProperties()
                        .getTitle()
                        .equals(sheetName)) {

                    return sheet.getProperties()
                            .getSheetId();
                }
            }
        }

        AddSheetRequest addSheetRequest =
                new AddSheetRequest()
                        .setProperties(
                                new SheetProperties()
                                        .setTitle(sheetName)
                        );

        BatchUpdateSpreadsheetRequest request =
                new BatchUpdateSpreadsheetRequest()
                        .setRequests(
                                Collections.singletonList(
                                        new Request()
                                                .setAddSheet(
                                                        addSheetRequest
                                                )
                                )
                        );

        BatchUpdateSpreadsheetResponse response =
                sheets.spreadsheets()
                        .batchUpdate(
                                spreadsheetId,
                                request
                        )
                        .execute();

        return response
                .getReplies()
                .get(0)
                .getAddSheet()
                .getProperties()
                .getSheetId();
    }


    // ============================================================
    // GET OR CREATE SUMMARY SHEET
    // ============================================================

    public static int getOrCreateSummarySheet(
            Sheets sheets,
            String spreadsheetId,
            String sheetName)
            throws Exception {

        Spreadsheet spreadsheet =
                sheets.spreadsheets()
                        .get(spreadsheetId)
                        .execute();

        if (spreadsheet.getSheets() != null) {

            for (Sheet sheet :
                    spreadsheet.getSheets()) {

                SheetProperties properties =
                        sheet.getProperties();

                if (properties == null) {
                    continue;
                }

                String existingName =
                        properties.getTitle();

                if (existingName != null &&
                        existingName.trim()
                                .equalsIgnoreCase(
                                        sheetName.trim()
                                )) {

                    System.out.println(
                            "Summary sheet found: "
                                    + existingName
                    );

                    return properties.getSheetId();
                }
            }
        }

        System.out.println(
                "Summary sheet not found. Creating: "
                        + sheetName
        );

        AddSheetRequest addSheetRequest =
                new AddSheetRequest()
                        .setProperties(
                                new SheetProperties()
                                        .setTitle(sheetName)
                        );

        BatchUpdateSpreadsheetRequest request =
                new BatchUpdateSpreadsheetRequest()
                        .setRequests(
                                Collections.singletonList(
                                        new Request()
                                                .setAddSheet(
                                                        addSheetRequest
                                                )
                                )
                        );

        BatchUpdateSpreadsheetResponse response =
                sheets.spreadsheets()
                        .batchUpdate(
                                spreadsheetId,
                                request
                        )
                        .execute();

        return response
                .getReplies()
                .get(0)
                .getAddSheet()
                .getProperties()
                .getSheetId();
    }


    // ============================================================
    // CLEAR SHEET
    // ============================================================

    public static void clearSheet(
            Sheets sheets,
            String spreadsheetId,
            String sheetName)
            throws Exception {

        sheets.spreadsheets()
                .values()
                .clear(
                        spreadsheetId,
                        sheetName + "!A:ZZ",
                        new ClearValuesRequest()
                )
                .execute();
    }


    // ============================================================
    // READ SUMMARY FROM COURSE SHEET
    // ============================================================

    public static List<CourseSummary>
    getSummariesFromSheet(
            String spreadsheetId,
            String tabName,
            CourseInfo course)
            throws Exception {

        Sheets sheets =
                createSheetsService();

        ValueRange response =
                sheets.spreadsheets()
                        .values()
                        .get(
                                spreadsheetId,
                                tabName + "!A2:D"
                        )
                        .setValueRenderOption(
                                "UNFORMATTED_VALUE"
                        )
                        .execute();

        List<List<Object>> rows =
                response.getValues();

        List<CourseSummary> summaries =
                new ArrayList<>();

        if (rows == null ||
                rows.isEmpty()) {

            return summaries;
        }

        String[] courseNameArray =
                course.name.split(" - ");

        String courseName =
                courseNameArray[0].trim();

        if (!courseName.isEmpty()) {

            courseName =
                    courseName.substring(1);
        }

        for (List<Object> row : rows) {

            if (row == null ||
                    row.size() < 4) {

                continue;
            }

            String rollNumber =
                    String.valueOf(
                            row.get(0)
                    );

            String studentName =
                    String.valueOf(
                            row.get(1)
                    );

            int submissions =
                    Integer.parseInt(
                            String.valueOf(
                                    row.get(2)
                            )
                    );

            double percentage =
                    Double.parseDouble(
                            String.valueOf(
                                    row.get(3)
                            )
                    );

            summaries.add(
                    new CourseSummary(
                            courseName,
                            studentName,
                            rollNumber,
                            submissions,
                            percentage
                    )
            );
        }

        System.out.println(
                "Summary data read from sheet: "
                        + tabName
                        + " -> "
                        + summaries.size()
                        + " students"
        );

        return summaries;
    }
}