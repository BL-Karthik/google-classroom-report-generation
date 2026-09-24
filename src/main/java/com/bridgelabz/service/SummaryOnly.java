package com.bridgelabz.service;

import com.bridgelabz.models.CourseSummary;
import com.bridgelabz.service.GoogleSheetService;
import com.bridgelabz.service.SummaryService;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.util.ArrayList;
import java.util.List;

public class SummaryOnly {

    private static final String SPREADSHEET_ID_B1 =
            "1fZsJPXt_6V5e7KnHO0MLE_ihbwrXtI3Ae0Md060bD2Q";

    private static final String SPREADSHEET_ID_B2 =
            "1jgmFkPIFFp71c2EPWMwCXOJ6bG_irJYxFn1rSAoTf_I";

    public static void main(String[] args) {

        System.out.println("\n======================================");
        System.out.println(" SUMMARY ONLY EXPORT");
        System.out.println("======================================");

        try {

            Sheets sheets =
                    GoogleSheetService.createSheetsService();

            // ====================================================
            // B1
            // ====================================================

            System.out.println("\n======================================");
            System.out.println("CREATING B1 SUMMARY");
            System.out.println("======================================");

            List<CourseSummary> b1Summaries =
                    readAllCourseSheets(
                            sheets,
                            SPREADSHEET_ID_B1,
                            "B1"
                    );

            System.out.println(
                    "\nB1 students collected: "
                            + b1Summaries.size()
            );

            if (!b1Summaries.isEmpty()) {

                boolean b1Success =
                        SummaryService.exportToSummarySheet(
                                SPREADSHEET_ID_B1,
                                "Summary",
                                b1Summaries
                        );

                System.out.println(
                        "B1 Summary export: "
                                + (b1Success
                                ? "SUCCESS"
                                : "FAILED")
                );

            } else {

                System.out.println(
                        "B1 Summary NOT exported because "
                                + "no students were collected."
                );
            }

            // ====================================================
            // B2
            // ====================================================

            System.out.println("\n======================================");
            System.out.println("CREATING B2 SUMMARY");
            System.out.println("======================================");

            List<CourseSummary> b2Summaries =
                    readAllCourseSheets(
                            sheets,
                            SPREADSHEET_ID_B2,
                            "B2"
                    );

            System.out.println(
                    "\nB2 students collected: "
                            + b2Summaries.size()
            );

            if (!b2Summaries.isEmpty()) {

                boolean b2Success =
                        SummaryService.exportToSummarySheet(
                                SPREADSHEET_ID_B2,
                                "Summary",
                                b2Summaries
                        );

                System.out.println(
                        "B2 Summary export: "
                                + (b2Success
                                ? "SUCCESS"
                                : "FAILED")
                );

            } else {

                System.out.println(
                        "B2 Summary NOT exported because "
                                + "no students were collected."
                );
            }

            System.out.println("\n======================================");
            System.out.println("SUMMARY EXPORT FINISHED");
            System.out.println("======================================");

        } catch (Exception e) {

            System.out.println("\n======================================");
            System.out.println("SUMMARY EXPORT FAILED");
            System.out.println("======================================");

            e.printStackTrace();
        }
    }


    // ============================================================
    // READ ALL COURSE SHEETS USING ONE BATCH GET
    // ============================================================

    private static List<CourseSummary> readAllCourseSheets(
            Sheets sheets,
            String spreadsheetId,
            String batchName) throws Exception {

        List<CourseSummary> summaries =
                new ArrayList<>();

        // --------------------------------------------------------
        // Get spreadsheet structure ONCE
        // --------------------------------------------------------

        Spreadsheet spreadsheet =
                sheets.spreadsheets()
                        .get(spreadsheetId)
                        .execute();

        if (spreadsheet.getSheets() == null) {
            return summaries;
        }

        List<String> sheetNames =
                new ArrayList<>();

        // --------------------------------------------------------
        // Find all course sheets
        // --------------------------------------------------------

        for (Sheet sheet :
                spreadsheet.getSheets()) {

            String sheetName =
                    sheet.getProperties()
                            .getTitle();

            if ("Summary".equalsIgnoreCase(sheetName)) {
                continue;
            }

            String upperName =
                    sheetName.toUpperCase();

            if (!upperName.startsWith(
                    batchName.toUpperCase())) {
                continue;
            }

            sheetNames.add(sheetName);
        }

        System.out.println(
                "Course sheets found: "
                        + sheetNames.size()
        );

        if (sheetNames.isEmpty()) {
            return summaries;
        }

        // --------------------------------------------------------
        // Create ranges for ONE batchGet request
        // --------------------------------------------------------

        List<String> ranges =
                new ArrayList<>();

        for (String sheetName :
                sheetNames) {

            ranges.add(
                    "'" + sheetName + "'!A2:D"
            );
        }

        System.out.println(
                "Reading all course sheets using "
                        + "one batch request..."
        );

        // --------------------------------------------------------
        // ONE API READ
        // --------------------------------------------------------

        List<ValueRange> valueRanges =
                sheets.spreadsheets()
                        .values()
                        .batchGet(spreadsheetId)
                        .setRanges(ranges)
                        .setValueRenderOption(
                                "UNFORMATTED_VALUE"
                        )
                        .execute()
                        .getValueRanges();

        // --------------------------------------------------------
        // Process returned ranges
        // --------------------------------------------------------

        for (int i = 0;
             i < valueRanges.size() && i < sheetNames.size();
             i++) {

            String sheetName =
                    sheetNames.get(i);

            ValueRange valueRange =
                    valueRanges.get(i);

            List<List<Object>> rows =
                    valueRange.getValues();

            if (rows == null || rows.isEmpty()) {
                continue;
            }

            int studentCount = 0;

            for (List<Object> row : rows) {

                if (row == null ||
                        row.size() < 4) {
                    continue;
                }

                String rollNumber =
                        String.valueOf(row.get(0));

                String studentName =
                        String.valueOf(row.get(1));

                int submissions =
                        parseInteger(row.get(2));

                double percentage =
                        parseDouble(row.get(3));

                CourseSummary courseSummary =
                        new CourseSummary(
                                sheetName,
                                studentName,
                                rollNumber,
                                submissions,
                                percentage
                        );

                summaries.add(courseSummary);

                studentCount++;
            }

            System.out.println(
                    sheetName
                            + " -> "
                            + studentCount
                            + " students"
            );
        }

        return summaries;
    }


    // ============================================================
    // INTEGER PARSER
    // ============================================================

    private static int parseInteger(Object value) {

        if (value == null) {
            return 0;
        }

        try {

            return Integer.parseInt(
                    String.valueOf(value)
            );

        } catch (NumberFormatException e) {

            try {

                return (int) Double.parseDouble(
                        String.valueOf(value)
                );

            } catch (NumberFormatException ignored) {

                return 0;
            }
        }
    }


    // ============================================================
    // DOUBLE PARSER
    // ============================================================

    private static double parseDouble(Object value) {

        if (value == null) {
            return 0;
        }

        try {

            return Double.parseDouble(
                    String.valueOf(value)
            );

        } catch (NumberFormatException e) {

            return 0;
        }
    }
}
