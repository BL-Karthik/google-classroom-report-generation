package com.bridgelabz.service;

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
            "1_ptnLvsphtf2WAER_oOBwCMcunKmvbguer0u_GUk_F8";

    private static final String SPREADSHEET_ID_B2 =
            "1waRl-38hE8NEROWbnTcJItDs8DuKnl6n-t107MWWa5w";

    private static final String SERVICE_ACCOUNT_FILE =
            "service_account.json";

    // ============================================================
    // GOOGLE SHEETS SERVICE
    // ============================================================

    public static Sheets createSheetsService() throws Exception {

        try (FileInputStream input =
                     new FileInputStream(SERVICE_ACCOUNT_FILE)) {

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
                    .setApplicationName("Classroom Grade Exporter")
                    .build();
        }
    }

    // ============================================================
    // TEST GOOGLE SHEETS ACCESS
    // ============================================================

    public static boolean testGoogleSheetsAccess() {

        System.out.println("\n======================================");
        System.out.println("GOOGLE SHEETS ACCESS TEST");
        System.out.println("======================================");

        try {

            Sheets sheets = createSheetsService();

            String[] targets = {
                    "B1: " + SPREADSHEET_ID_B1,
                    "B2: " + SPREADSHEET_ID_B2
            };

            boolean allOk = true;

            for (String target : targets) {

                String id = target.substring(
                        target.indexOf(':') + 2
                );

                System.out.println(
                        "\nChecking spreadsheet: "
                                + target
                );

                if (id.contains("HERE")) {

                    System.out.println(
                            "NOT CONFIGURED - paste the "
                                    + "spreadsheet ID."
                    );

                    allOk = false;
                    continue;
                }

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

            if (e.getStatusCode() == 404) {

                System.out.println(
                        "\n404 - Spreadsheet not found."
                );

                System.out.println(
                        "\nCHECK THESE:"
                );

                System.out.println(
                        "1. Spreadsheet ID is correct."
                );

                System.out.println(
                        "2. Spreadsheet is shared with the service account."
                );

                System.out.println(
                        "3. Service account has Editor permission."
                );

                System.out.println(
                        "4. service_account.json is correct."
                );

                System.out.println(
                        "5. Google Sheets API is enabled."
                );
            }

            if (e.getStatusCode() == 403) {

                System.out.println(
                        "\n403 - Permission denied."
                );

                System.out.println(
                        "Give the service account Editor access."
                );
            }

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
    // CHECK WHETHER ASSIGNMENT IS WEEK 1 - WEEK 6
    // ============================================================

    private static boolean isWeek1To6(String title) {

        if (title == null || title.trim().isEmpty()) {
            return false;
        }

        /*
         * Accepts:
         *
         * Week 1
         * Week 2 - Java
         * Java - Week 3
         * Assignment - Week 4
         * JFS Week 5
         * Week 6 Assignment
         *
         * Rejects:
         *
         * Week 7
         * Week 8
         * Week 10
         * Project
         * NPS Feedback
         */

        return title.matches(
                "(?i).*\\bweek\\s*[1-6]\\b.*"
        );
    }

    // ============================================================
    // GET ONLY WEEK 1 - WEEK 6 ASSIGNMENTS
    // ============================================================

    private static List<AssignmentColumn> getWeek1To6Assignments(
            List<AssignmentColumn> assignments) {

        List<AssignmentColumn> validAssignments =
                new ArrayList<>();

        if (assignments == null) {
            return validAssignments;
        }

        for (AssignmentColumn assignment : assignments) {

            if (assignment == null) {
                continue;
            }

            if (isWeek1To6(assignment.title)) {

                validAssignments.add(assignment);
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

            Sheets sheets = createSheetsService();

            String sheetName = tabName;

            int sheetId =
                    GoogleSheetService.getOrCreateSheet(
                            sheets,
                            spreadsheetId,
                            sheetName
                    );

            GoogleSheetService.clearSheet(
                    sheets,
                    spreadsheetId,
                    sheetName
            );

            // ====================================================
            // IMPORTANT:
            // ONLY WEEK 1 - WEEK 6
            // ====================================================

            List<AssignmentColumn> validAssignments =
                    getWeek1To6Assignments(assignments);

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

            // ====================================================
            // CREATE HEADER
            // ====================================================

            List<List<Object>> values =
                    new ArrayList<>();

            List<Object> header =
                    new ArrayList<>();

            header.add("# Roll Number");
            header.add("Student Name");
            header.add("Submissions");
            header.add("Percentage");

            /*
             * Status columns:
             *
             * Week 1
             * Week 2
             * Week 3
             * ...
             * Week 6
             *
             * Only the assignments actually present
             * in Classroom are included.
             */

            for (AssignmentColumn assignment :
                    validAssignments) {

                header.add(
                        assignment.title
                                + " - Status"
                );
            }

            values.add(header);

            // ====================================================
            // COLUMN LETTERS
            // ====================================================

            /*
             * A = Roll Number
             * B = Student Name
             * C = Submissions
             * D = Percentage
             * E onwards = Status
             */

            String firstStatusCol =
                    columnLetter(4);

            String lastStatusCol =
                    columnLetter(
                            4 + statusCount - 1
                    );

            String submissionsCol =
                    columnLetter(2);

            // ====================================================
            // STUDENTS
            // ====================================================

            for (StudentData student : students) {

                List<Object> row =
                        new ArrayList<>();

                row.add(student.rollNumber);
                row.add(student.name);

                int rowNum =
                        values.size() + 1;

                // =================================================
                // SUBMISSIONS + PERCENTAGE
                // =================================================

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

                // =================================================
                // STATUS VALUES
                // =================================================

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

            // ====================================================
            // WRITE TO GOOGLE SHEETS
            // ====================================================

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

            // ====================================================
            // FORMAT SHEET
            // ====================================================

            GoogleSheetService.formatSheet(
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

            if (e.getStatusCode() == 404) {

                System.out.println(
                        "Spreadsheet not found or service account "
                                + "does not have access."
                );
            }

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
    // GET STUDENT STATUS FOR ASSIGNMENT
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
                assignments.indexOf(assignment);

        if (originalIndex >= 0 &&
                originalIndex < student.assignments.size()) {

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
    // FIND ASSIGNMENT INDEX
    // ============================================================

    private static int findAssignmentIndex(
            AssignmentColumn assignment) {

        /*
         * AssignmentColumn.cellIndex represents
         * the original Classroom column position.
         *
         * We cannot directly use cellIndex here as
         * StudentData.assignments may contain only
         * processed assignments.
         *
         * The title-based fallback above handles
         * the safer matching case.
         */

        return -1;
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
            String sheetName) throws Exception {

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
    // CLEAR SHEET
    // ============================================================

    // ============================================================
// GET EXISTING SHEET ID
// ============================================================

    public static int getOrCreateSummarySheet(
            Sheets sheets,
            String spreadsheetId,
            String sheetName) throws Exception {

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
                                        sheetName.trim())) {

                    System.out.println(
                            "Summary sheet found: "
                                    + existingName
                    );

                    return properties.getSheetId();
                }
            }
        }

        // Summary sheet does not exist.
        // Create it.
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
    public static void clearSheet(
            Sheets sheets,
            String spreadsheetId,
            String sheetName) throws Exception {

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
    // FORMAT INDIVIDUAL SHEET
    // ============================================================

    public static void formatSheet(
            Sheets sheets,
            String spreadsheetId,
            int sheetId,
            int rows,
            int columns) throws Exception {

        List<Request> requests =
                new ArrayList<>();

        // ========================================================
        // BOLD HEADER
        // ========================================================

        requests.add(
                new Request()
                        .setRepeatCell(
                                new RepeatCellRequest()
                                        .setRange(
                                                new GridRange()
                                                        .setSheetId(
                                                                sheetId
                                                        )
                                                        .setStartRowIndex(
                                                                0
                                                        )
                                                        .setEndRowIndex(
                                                                1
                                                        )
                                                        .setStartColumnIndex(
                                                                0
                                                        )
                                                        .setEndColumnIndex(
                                                                columns
                                                        )
                                        )
                                        .setCell(
                                                new CellData()
                                                        .setUserEnteredFormat(
                                                                new CellFormat()
                                                                        .setTextFormat(
                                                                                new TextFormat()
                                                                                        .setBold(true)
                                                                        )
                                                        )
                                        )
                                        .setFields(
                                                "userEnteredFormat.textFormat.bold"
                                        )
                        )
        );

        // ========================================================
        // FREEZE FIRST ROW
        // ========================================================

        requests.add(
                new Request()
                        .setUpdateSheetProperties(
                                new UpdateSheetPropertiesRequest()
                                        .setProperties(
                                                new SheetProperties()
                                                        .setSheetId(
                                                                sheetId
                                                        )
                                                        .setGridProperties(
                                                                new GridProperties()
                                                                        .setFrozenRowCount(
                                                                                1
                                                                        )
                                                        )
                                        )
                                        .setFields(
                                                "gridProperties.frozenRowCount"
                                        )
                        )
        );

        // ========================================================
        // AUTO RESIZE
        // ========================================================

        requests.add(
                new Request()
                        .setAutoResizeDimensions(
                                new AutoResizeDimensionsRequest()
                                        .setDimensions(
                                                new DimensionRange()
                                                        .setSheetId(
                                                                sheetId
                                                        )
                                                        .setDimension(
                                                                "COLUMNS"
                                                        )
                                                        .setStartIndex(
                                                                0
                                                        )
                                                        .setEndIndex(
                                                                columns
                                                        )
                                        )
                        )
        );

        // ========================================================
        // PERCENTAGE FORMAT
        // ========================================================

        requests.add(
                new Request()
                        .setRepeatCell(
                                new RepeatCellRequest()
                                        .setRange(
                                                new GridRange()
                                                        .setSheetId(
                                                                sheetId
                                                        )
                                                        .setStartRowIndex(
                                                                1
                                                        )
                                                        .setEndRowIndex(
                                                                rows
                                                        )
                                                        .setStartColumnIndex(
                                                                3
                                                        )
                                                        .setEndColumnIndex(
                                                                4
                                                        )
                                        )
                                        .setCell(
                                                new CellData()
                                                        .setUserEnteredFormat(
                                                                new CellFormat()
                                                                        .setNumberFormat(
                                                                                new NumberFormat()
                                                                                        .setType(
                                                                                                "PERCENT"
                                                                                        )
                                                                                        .setPattern(
                                                                                                "0%"
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .setFields(
                                                "userEnteredFormat.numberFormat"
                                        )
                        )
        );

        sheets.spreadsheets()
                .batchUpdate(
                        spreadsheetId,
                        new BatchUpdateSpreadsheetRequest()
                                .setRequests(requests)
                )
                .execute();
    }

    // ============================================================
    // SUMMARY SHEET FORMAT
    // ============================================================

    public static void summaryFormatSheet(
            Sheets sheets,
            String spreadsheetId,
            int sheetId,
            int rows,
            int columns) throws Exception {

        List<Request> requests =
                new ArrayList<>();

        // Bold header
        requests.add(
                new Request()
                        .setRepeatCell(
                                new RepeatCellRequest()
                                        .setRange(
                                                new GridRange()
                                                        .setSheetId(
                                                                sheetId
                                                        )
                                                        .setStartRowIndex(
                                                                0
                                                        )
                                                        .setEndRowIndex(
                                                                1
                                                        )
                                                        .setStartColumnIndex(
                                                                0
                                                        )
                                                        .setEndColumnIndex(
                                                                columns
                                                        )
                                        )
                                        .setCell(
                                                new CellData()
                                                        .setUserEnteredFormat(
                                                                new CellFormat()
                                                                        .setTextFormat(
                                                                                new TextFormat()
                                                                                        .setBold(true)
                                                                        )
                                                        )
                                        )
                                        .setFields(
                                                "userEnteredFormat.textFormat.bold"
                                        )
                        )
        );

        // Freeze first row
        requests.add(
                new Request()
                        .setUpdateSheetProperties(
                                new UpdateSheetPropertiesRequest()
                                        .setProperties(
                                                new SheetProperties()
                                                        .setSheetId(
                                                                sheetId
                                                        )
                                                        .setGridProperties(
                                                                new GridProperties()
                                                                        .setFrozenRowCount(
                                                                                1
                                                                        )
                                                        )
                                        )
                                        .setFields(
                                                "gridProperties.frozenRowCount"
                                        )
                        )
        );

        // Auto resize
        requests.add(
                new Request()
                        .setAutoResizeDimensions(
                                new AutoResizeDimensionsRequest()
                                        .setDimensions(
                                                new DimensionRange()
                                                        .setSheetId(
                                                                sheetId
                                                        )
                                                        .setDimension(
                                                                "COLUMNS"
                                                        )
                                                        .setStartIndex(
                                                                0
                                                        )
                                                        .setEndIndex(
                                                                columns
                                                        )
                                        )
                        )
        );

        // Percentage
        requests.add(
                new Request()
                        .setRepeatCell(
                                new RepeatCellRequest()
                                        .setRange(
                                                new GridRange()
                                                        .setSheetId(
                                                                sheetId
                                                        )
                                                        .setStartRowIndex(
                                                                1
                                                        )
                                                        .setEndRowIndex(
                                                                rows
                                                        )
                                                        .setStartColumnIndex(
                                                                4
                                                        )
                                                        .setEndColumnIndex(
                                                                5
                                                        )
                                        )
                                        .setCell(
                                                new CellData()
                                                        .setUserEnteredFormat(
                                                                new CellFormat()
                                                                        .setNumberFormat(
                                                                                new NumberFormat()
                                                                                        .setType(
                                                                                                "PERCENT"
                                                                                        )
                                                                                        .setPattern(
                                                                                                "0%"
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .setFields(
                                                "userEnteredFormat.numberFormat"
                                        )
                        )
        );

        // Submissions number format
        requests.add(
                new Request()
                        .setRepeatCell(
                                new RepeatCellRequest()
                                        .setRange(
                                                new GridRange()
                                                        .setSheetId(
                                                                sheetId
                                                        )
                                                        .setStartRowIndex(
                                                                1
                                                        )
                                                        .setEndRowIndex(
                                                                rows
                                                        )
                                                        .setStartColumnIndex(
                                                                3
                                                        )
                                                        .setEndColumnIndex(
                                                                4
                                                        )
                                        )
                                        .setCell(
                                                new CellData()
                                                        .setUserEnteredFormat(
                                                                new CellFormat()
                                                                        .setNumberFormat(
                                                                                new NumberFormat()
                                                                                        .setType(
                                                                                                "NUMBER"
                                                                                        )
                                                                                        .setPattern(
                                                                                                "0"
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .setFields(
                                                "userEnteredFormat.numberFormat"
                                        )
                        )
        );

        sheets.spreadsheets()
                .batchUpdate(
                        spreadsheetId,
                        new BatchUpdateSpreadsheetRequest()
                                .setRequests(requests)
                )
                .execute();
    }

    // ============================================================
    // READ SUMMARY FROM SHEET
    // ============================================================

    public static List<CourseSummary> getSummariesFromSheet(
            String spreadsheetId,
            String tabName,
            CourseInfo course) throws Exception {

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

        if (rows == null || rows.isEmpty()) {
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

            if (row.size() < 4) {
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

            CourseSummary summary =
                    new CourseSummary(
                            courseName,
                            studentName,
                            rollNumber,
                            submissions,
                            percentage
                    );

            summaries.add(summary);
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

    // ============================================================
// ENSURE SHEET HAS ENOUGH ROWS AND COLUMNS
// ============================================================

    public static void ensureSheetSize(
            Sheets sheets,
            String spreadsheetId,
            int sheetId,
            int requiredRows,
            int requiredColumns) throws Exception {

        Spreadsheet spreadsheet =
                sheets.spreadsheets()
                        .get(spreadsheetId)
                        .execute();

        for (Sheet sheet : spreadsheet.getSheets()) {

            SheetProperties properties =
                    sheet.getProperties();

            if (properties.getSheetId() != sheetId) {
                continue;
            }

            GridProperties gridProperties =
                    properties.getGridProperties();

            int currentRows =
                    gridProperties.getRowCount();

            int currentColumns =
                    gridProperties.getColumnCount();

            boolean needsUpdate =
                    currentRows < requiredRows
                            || currentColumns < requiredColumns;

            if (!needsUpdate) {
                return;
            }

            GridProperties newGridProperties =
                    new GridProperties()
                            .setRowCount(
                                    Math.max(
                                            currentRows,
                                            requiredRows
                                    )
                            )
                            .setColumnCount(
                                    Math.max(
                                            currentColumns,
                                            requiredColumns
                                    )
                            );

            SheetProperties newProperties =
                    new SheetProperties()
                            .setSheetId(sheetId)
                            .setGridProperties(
                                    newGridProperties
                            );

            UpdateSheetPropertiesRequest updateRequest =
                    new UpdateSheetPropertiesRequest()
                            .setProperties(newProperties)
                            .setFields(
                                    "gridProperties.rowCount,"
                                            + "gridProperties.columnCount"
                            );

            Request request =
                    new Request()
                            .setUpdateSheetProperties(
                                    updateRequest
                            );

            sheets.spreadsheets()
                    .batchUpdate(
                            spreadsheetId,
                            new BatchUpdateSpreadsheetRequest()
                                    .setRequests(
                                            Collections.singletonList(
                                                    request
                                            )
                                    )
                    )
                    .execute();

            System.out.println(
                    "Sheet resized: "
                            + requiredRows
                            + " rows x "
                            + requiredColumns
                            + " columns"
            );

            return;
        }

        throw new IllegalStateException(
                "Sheet ID not found: " + sheetId
        );
    }
}