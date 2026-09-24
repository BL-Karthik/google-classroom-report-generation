
package com.bridgelabz.service;

import com.bridgelabz.models.*;
        import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.util.ArrayList;
import java.util.List;

import static com.bridgelabz.service.GoogleSheetService.createSheetsService;

public class SummaryService {

    public static CourseSummary createSummary(
            CourseInfo course,
            StudentData student) {

        String[] courseNameArray =
                course.name.split(" - ");

        String courseName =
                courseNameArray[0].trim();

        if (courseName.length() > 1) {
            courseName =
                    courseName.substring(1);
        }

        String studentName =
                student.name;

        String studentRollNumber =
                student.rollNumber;

        int submitted = 0;
        int totalAssignments = 0;

        List<AssignmentResult> list =
                student.assignments;

        /*
         * Count ONLY Week 1 to config week #
         *
         * Week 7, Week 8, projects, feedback,
         * quizzes, etc. are ignored.
         *
         * If Week 3 was not posted, it is simply
         * not counted in the denominator.
         */
        for (AssignmentResult assignment :
                list) {

            if (!GoogleSheetService.isValidWeekAssignment(assignment.title)) {
                continue;
            }

            totalAssignments++;

            if ("Submitted".equalsIgnoreCase(
                    assignment.status)) {

                submitted++;
            }
        }

        double percentageOfSubmissions =
                totalAssignments == 0
                        ? 0
                        : (double) submitted
                        / totalAssignments;

        return new CourseSummary(
                courseName,
                studentName,
                studentRollNumber,
                submitted,
                percentageOfSubmissions
        );
    }

    public static boolean exportToSummarySheet(
            String spreadsheetId,
            String tabName,
            List<CourseSummary> summary) {

        try {

            Sheets sheets =
                    createSheetsService();

            String sheetName = tabName;

            int sheetId =
                    GoogleSheetService.getOrCreateSummarySheet(
                            sheets,
                            spreadsheetId,
                            sheetName
                    );
            GoogleSheetFormatService.ensureSheetSize(
                    sheets,
                    spreadsheetId,
                    sheetId,
                    summary.size() + 1,
                    5
            );

            GoogleSheetService.clearSheet(
                    sheets,
                    spreadsheetId,
                    sheetName
            );

            List<List<Object>> values =
                    new ArrayList<>();

            List<Object> header =
                    new ArrayList<>();

            header.add("# Course Name");
            header.add("# Roll Number");
            header.add("Student Name");
            header.add("Submissions");
            header.add("Percentage");

            values.add(header);

            for (CourseSummary courseSummary :
                    summary) {

                List<Object> row =
                        new ArrayList<>();

                row.add(
                        courseSummary.getCourseName()
                );

                row.add(
                        courseSummary.getRollNumber()
                );

                row.add(
                        courseSummary.getName()
                );

                row.add(
                        courseSummary.getSubmissions()
                );

                row.add(
                        courseSummary
                                .getPercentageSubmissions()
                );

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

            GoogleSheetFormatService.summaryFormatSheet(
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

        }catch (GoogleJsonResponseException e) {
            System.out.println("Google Sheets export failed.");
            System.out.println("HTTP Status: " + e.getStatusCode());
            System.out.println("Error message: " + e.getDetails());
            e.printStackTrace();

            if (e.getStatusCode() == 404) {

                System.out.println(
                        "Spreadsheet not found or "
                                + "service account does not "
                                + "have access."
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
}
