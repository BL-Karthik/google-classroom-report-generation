package com.bridgelabz.service;

import com.bridgelabz.models.AssignmentColumn;
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
            "1aynR_u0F0kOkDgnSet3814jasrmuk1btMtdfDOD72ac";

    private static final String SPREADSHEET_ID_B2 =
            "1hYUmjJ3V6gfguvcHCZJFkmLN7Gi5Yd2YBg6xeDJHFEk";

    private static final String SERVICE_ACCOUNT_FILE =
            "service_account.json";

    // ============================================================
    // GOOGLE SHEETS
    // ============================================================

    public  static Sheets createSheetsService() throws Exception {

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

    public  static boolean testGoogleSheetsAccess() {

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
                        target.indexOf(':') + 2);

                System.out.println(
                        "\nChecking spreadsheet: "
                                + target
                );

                if (id.contains("HERE")) {

                    System.out.println(
                            "NOT CONFIGURED - paste the "
                                    + "spreadsheet ID into "
                                    + "ClassroomMultiExport.java"
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

        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {

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

            List<List<Object>> values =
                    new ArrayList<>();

            // Every coursework exports as a single
            // Status column (grades are tracked in
            // the Classroom itself), followed by two
            // summary columns.
            int statusCount = Math.min(assignments.size() - 4, 10);

            List<Object> header =
                    new ArrayList<>();
            header.add("# Roll Number ");
            header.add("Student Name");
            header.add("Submissions");
            header.add("Percentage");


            for (int i = assignments.size()-1;
                 i >=assignments.size()-statusCount;
                 i--) {

                header.add(
                        assignments.get(i).title
                                + " - Status"
                );
            }



            values.add(header);

            // 0-based indexes of the first status
            // column (B), the Submissions column and
            // the Percentage column in each row.
            String firstStatusCol =
                    GoogleSheetService.columnLetter(4);

            String lastStatusCol =
                    GoogleSheetService.columnLetter(4+statusCount-1);

            String submissionsCol =
                    GoogleSheetService.columnLetter(2);

            // STUDENTS
            for (StudentData student : students) {

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
                                    + rowNum + ":"
                                    + lastStatusCol
                                    + rowNum
                                    + ",\"Submitted\")"
                    );

                    row.add(
                            "=" + submissionsCol
                                    + rowNum
                                    + "/" + statusCount
                    );

                } else {

                    row.add(0);

                    row.add("");
                }

                for (int i = student.assignments.size()-1;
                     i >=assignments.size()-statusCount;
                     i--) {

                    row.add(
                            student.assignments
                                    .get(i)
                                    .status
                    );
                }

                values.add(row);
            }


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

    /*
     * Converts a 0-based column index into its
     * spreadsheet letter, e.g. 0 -> "A",
     * 27 -> "AB" (used to build the summary
     * column formulas).
     */
    public  static String columnLetter(
            int zeroBasedIndex) {

        StringBuilder letter =
                new StringBuilder();

        int index = zeroBasedIndex;

        while (index >= 0) {

            letter.insert(
                    0,
                    (char) ('A' + index % 26)
            );

            index = index / 26 - 1;
        }

        return letter.toString();
    }

    public  static int getOrCreateSheet(
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

    public  static void clearSheet(
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

    public  static void formatSheet(
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
                                                        .setSheetId(sheetId)
                                                        .setStartRowIndex(0)
                                                        .setEndRowIndex(1)
                                                        .setStartColumnIndex(0)
                                                        .setEndColumnIndex(columns)
                                        )
                                        .setCell(
                                                new com.google.api.services.sheets.v4.model.CellData()
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
                                                        .setSheetId(sheetId)
                                                        .setGridProperties(
                                                                new GridProperties()
                                                                        .setFrozenRowCount(1)
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
                                                        .setSheetId(sheetId)
                                                        .setDimension("COLUMNS")
                                                        .setStartIndex(0)
                                                        .setEndIndex(columns)
                                        )
                        )
        );

        // Percent format on the Percentage
        // column (last header column) so the
        // formula shows e.g. "70%" not "0.7".
        requests.add(
                new Request()
                        .setRepeatCell(
                                new RepeatCellRequest()
                                        .setRange(
                                                new GridRange()
                                                        .setSheetId(sheetId)
                                                        .setStartRowIndex(1)
                                                        .setEndRowIndex(rows)
                                                        .setStartColumnIndex(3)
                                                        .setEndColumnIndex(4)
                                        )
                                        .setCell(
                                                new com.google.api.services.sheets.v4.model.CellData()
                                                        .setUserEnteredFormat(
                                                                new CellFormat()
                                                                        .setNumberFormat(
                                                                                new NumberFormat()
                                                                                        .setType("PERCENT")
                                                                                        .setPattern("0%")
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

    public  static void summaryFormatSheet(
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
                                                        .setSheetId(sheetId)
                                                        .setStartRowIndex(0)
                                                        .setEndRowIndex(1)
                                                        .setStartColumnIndex(0)
                                                        .setEndColumnIndex(columns)
                                        )
                                        .setCell(
                                                new com.google.api.services.sheets.v4.model.CellData()
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
                                                        .setSheetId(sheetId)
                                                        .setGridProperties(
                                                                new GridProperties()
                                                                        .setFrozenRowCount(1)
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
                                                        .setSheetId(sheetId)
                                                        .setDimension("COLUMNS")
                                                        .setStartIndex(0)
                                                        .setEndIndex(columns)
                                        )
                        )
        );

        // Percent format on the Percentage
        // column (last header column) so the
        // formula shows e.g. "70%" not "0.7".
        requests.add(
                new Request()
                        .setRepeatCell(
                                new RepeatCellRequest()
                                        .setRange(
                                                new GridRange()
                                                        .setSheetId(sheetId)
                                                        .setStartRowIndex(1)
                                                        .setEndRowIndex(rows)
                                                        .setStartColumnIndex(4)
                                                        .setEndColumnIndex(5)
                                        )
                                        .setCell(
                                                new com.google.api.services.sheets.v4.model.CellData()
                                                        .setUserEnteredFormat(
                                                                new CellFormat()
                                                                        .setNumberFormat(
                                                                                new NumberFormat()
                                                                                        .setType("PERCENT")
                                                                                        .setPattern("0%")
                                                                        )
                                                        )
                                        )
                                        .setFields(
                                                "userEnteredFormat.numberFormat"
                                        )
                        )
        );

        requests.add(
                new Request()
                        .setRepeatCell(
                                new RepeatCellRequest()
                                        .setRange(
                                                new GridRange()
                                                        .setSheetId(sheetId)
                                                        .setStartRowIndex(1)
                                                        .setEndRowIndex(rows)
                                                        .setStartColumnIndex(3)
                                                        .setEndColumnIndex(4)
                                        )
                                        .setCell(
                                                new com.google.api.services.sheets.v4.model.CellData()
                                                        .setUserEnteredFormat(
                                                                new CellFormat()
                                                                        .setNumberFormat(
                                                                                new NumberFormat()
                                                                                        .setType("NUMBER")

                                                                                        .setPattern("0")
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
}
