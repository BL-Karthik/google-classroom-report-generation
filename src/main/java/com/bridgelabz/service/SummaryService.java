package com.bridgelabz.service;

import com.bridgelabz.models.*;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SummaryService {


    public static CourseSummary createSummary(CourseInfo course, StudentData student)
    {
        String [] courseNameArray=course.name.split(" - ");
        String courseName=courseNameArray[0].trim().substring(1);
        String studentName=student.name;
        String studentRollNumber=student.rollNumber;

        double percentage_of_submissions;

        int submitted=0;

        List<AssignmentResult> list= student.assignments;


        int totalAssignments = Math.min(10, list.size());

        for (int i = 0; i < totalAssignments; i++) {

            AssignmentResult assignmentColumn = list.get(i);

            if ("Submitted".equals(assignmentColumn.status)) {
                submitted++;
            }
        }

         percentage_of_submissions =
                totalAssignments == 0
                        ? 0
                        : (double) submitted / totalAssignments;

        return new CourseSummary(courseName,
                studentName,studentRollNumber,
                submitted,
                percentage_of_submissions);

    }


    public static boolean exportToSummarySheet( String spreadsheetId, String tabName,
                                                List<CourseSummary> summary)

    {

        try {

            Sheets sheets = GoogleSheetService.createSheetsService();

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
            List<Object> header =
                    new ArrayList<>();
            header.add("# Course Name");
            header.add("# Roll Number ");
            header.add("Student Name");
            header.add("Submissions");
            header.add("Percentage");

            values.add(header);

            for (CourseSummary courseSummary : summary) {
                List<Object> row =
                        new ArrayList<>();
                row.add(courseSummary.getCourseName());
                row.add(courseSummary.getRollNumber());
                row.add(courseSummary.getName());
                row.add(courseSummary.getSubmissions());
                row.add(courseSummary.getPercentageSubmissions());
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

            GoogleSheetService.summaryFormatSheet(
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
}
